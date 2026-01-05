package com.fam.vest.service.implementation;

import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.exception.InternalException;
import com.fam.vest.repository.ApplicationUserRepository;
import com.fam.vest.repository.ApplicationUserTradingAccountMappingRepository;
import com.fam.vest.repository.TradingAccountRepository;
import com.fam.vest.service.TokenService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class ITokenService implements TokenService {

    // Parameter constants
    private static final String PARAM_NAME = "--name";
    private static final String PARAM_USER_ID = "--user_id";
    private static final String PARAM_PASSWORD = "--password";
    private static final String PARAM_TOTP_KEY = "--totp_key";
    private static final String PARAM_API_KEY = "--api_key";
    private static final String PARAM_API_SECRET = "--api_secret";
    private static final String PARAM_REQUEST_TOKEN_URL = "--request_token_url";

    private final TradingAccountRepository tradingAccountRepository;
    private final ApplicationUserRepository applicationUserRepository;
    private final ApplicationUserTradingAccountMappingRepository applicationUserTradingAccountMappingRepository;

    @Autowired
    public ITokenService(TradingAccountRepository tradingAccountRepository,
                         ApplicationUserRepository applicationUserRepository,
                         ApplicationUserTradingAccountMappingRepository applicationUserTradingAccountMappingRepository
    ) {
        this.tradingAccountRepository = tradingAccountRepository;
        this.applicationUserRepository = applicationUserRepository;
        this.applicationUserTradingAccountMappingRepository = applicationUserTradingAccountMappingRepository;
    }

    @Value("${fam.vest.app.python.path:python3}")
    private String pythonPath;

    @Value("${fam.vest.app.kite.login.endpoint}")
    private String kiteLoginEndpoint;


    private List<TradingAccount> getTradingAccounts(UserDetails userDetails, boolean isOnApplicationStart) {
        if(isOnApplicationStart) {
            return tradingAccountRepository.findAllByOrderByIdAsc();
        } else {
            ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userDetails.getUsername());
            return applicationUserTradingAccountMappingRepository.findTradingAccountsByApplicationUserId(applicationUser.getId());
        }
    }

    private TradingAccount getTradingAccount(String tradingAccountUserId) {
        return tradingAccountRepository.findTradingAccountByUserId(tradingAccountUserId);
    }

    enum SCRIPT_TYPE {
        RENEW_TOKENS,
        GET_ENC_TOKEN,
        GET_TOTP
    }

    @Override
    public void renewRequestToken(String tradingAccountUserId) {
        log.info("Trying to renew request token for user: {}", tradingAccountUserId);
        TradingAccount tradingAccount = this.getTradingAccount(tradingAccountUserId);
        if(null != tradingAccount) {
            this.runScriptToRenewRequestToken(tradingAccount);
        } else {
            log.error("Trading account not found for user: {}", tradingAccountUserId);
        }
    }

    @Override
    public String renewRequestTokens(UserDetails userDetails, boolean isOnApplicationStart) {
        log.info("Renewing request tokens for the requested users");
        List<TradingAccount> tradingAccounts = this.getTradingAccounts(userDetails, isOnApplicationStart);

        if (tradingAccounts == null || tradingAccounts.isEmpty()) {
            log.warn("No trading accounts found for renewal");
            return "No trading accounts found for renewal";
        }

        // Create a dynamic thread pool based on the number of accounts
        int poolSize = Math.min(tradingAccounts.size(), Math.max(4, Runtime.getRuntime().availableProcessors()));
        log.info("Creating thread pool with {} threads for {} trading accounts", poolSize, tradingAccounts.size());

        ExecutorService executor = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            private final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("token-renewal-" + count.incrementAndGet());
                t.setDaemon(false);
                return t;
            }
        });

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (TradingAccount tradingAccount : tradingAccounts) {
            if (null != tradingAccount) {
                // Submit each renewal task to the executor service for parallel execution
                CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> this.runScriptToRenewRequestToken(tradingAccount),
                    executor
                );
                futures.add(future);
            }
        }

        // Wait for all futures to complete with a reasonable timeout
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, TimeUnit.MINUTES);
            log.info("Request token renewal completed for all {} users", futures.size());
            return "Request token renewal completed for all users. Please verify the status on dashboard.";
        } catch (TimeoutException e) {
            log.error("Token renewal timed out after 5 minutes for some accounts", e);
            return "Request token renewal timed out. Some accounts may not have completed renewal.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Token renewal was interrupted", e);
            return "Request token renewal was interrupted";
        } catch (ExecutionException e) {
            log.error("Error during token renewal execution", e.getCause());
            return "Request token renewal completed with errors. Please verify the status on dashboard.";
        } finally {
            // Shutdown the executor to free resources
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("Executor did not terminate, forcing shutdown");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("Executor shutdown was interrupted", e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void runScriptToRenewRequestToken(TradingAccount tradingAccount) {
        if(StringUtils.isNotBlank(tradingAccount.getPassword()) && StringUtils.isNotBlank(tradingAccount.getTotpKey())) {
            Map<String, String> params = new HashMap<>();
            params.put(PARAM_NAME, tradingAccount.getName());
            params.put(PARAM_USER_ID, tradingAccount.getUserId());
            params.put(PARAM_PASSWORD, tradingAccount.getPassword());
            params.put(PARAM_TOTP_KEY, tradingAccount.getTotpKey());
            params.put(PARAM_API_KEY, tradingAccount.getApiKey());
            params.put(PARAM_API_SECRET, tradingAccount.getApiSecret());
            params.put(PARAM_REQUEST_TOKEN_URL, kiteLoginEndpoint);
            String resetTokensPythonScriptPath = this.getScriptFromResources("scripts/python/reset_req_tokens.py");
            if(null == resetTokensPythonScriptPath) {
                log.error("Reset access token python script path is null. Cannot proceed with token renewal for user: {}", tradingAccount.getUserId());
                return;
            }
            this.runPythonScript(SCRIPT_TYPE.RENEW_TOKENS, resetTokensPythonScriptPath, params);
            log.info("Request token renewal completed for user: {}", tradingAccount.getUserId());
        } else {
            log.warn("Password and/or TOTP key not found. Request token renewal for user: {} is skipped.", tradingAccount.getUserId());
        }
    }

    @Override
    public String getENCToken(TradingAccount tradingAccount) {
        log.info("Getting enc token for user: {}", tradingAccount.getUserId());
        String encToken = null;
        if(StringUtils.isNotBlank(tradingAccount.getPassword()) && StringUtils.isNotBlank(tradingAccount.getTotpKey())) {
            Map<String, String> params = new HashMap<>();
            params.put(PARAM_USER_ID, tradingAccount.getUserId());
            params.put(PARAM_PASSWORD, tradingAccount.getPassword());
            params.put(PARAM_TOTP_KEY, tradingAccount.getTotpKey());
            String getEncTokenPythonScriptPath = this.getScriptFromResources("scripts/python/get_enc_token.py");
            if(null == getEncTokenPythonScriptPath) {
                log.error("Enc token python script path is null. Cannot proceed with token renewal for user: {}", tradingAccount.getUserId());
                return null;
            }
            encToken = this.runPythonScript(SCRIPT_TYPE.GET_ENC_TOKEN, getEncTokenPythonScriptPath, params);
        } else {
            log.error("Password and/or TOTP key not found. ENC token generation for user: {} is skipped.", tradingAccount.getUserId());
        }
        return encToken;
    }

    @Override
    public String getTotp(TradingAccount tradingAccount) {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_TOTP_KEY, tradingAccount.getTotpKey());
        params.put(PARAM_USER_ID, tradingAccount.getUserId());
        String getTotpPythonScriptPath = this.getScriptFromResources("scripts/python/get_totp.py");
        if(null == getTotpPythonScriptPath) {
            log.error("Get TOTP python script path is null. Cannot proceed.");
            throw new InternalException("Get TOTP script not found");
        }
        String totpPin = this.runPythonScript(SCRIPT_TYPE.GET_TOTP, getTotpPythonScriptPath, params);
        log.debug("TOTP PIN retrieved successfully");
        return totpPin;
    }

    private String runPythonScript(SCRIPT_TYPE scriptType, String scriptPath, Map<String, String> params) {
        StringBuilder output = new StringBuilder();
        try {
            log.debug("Running python script for user: {}", params.get(PARAM_USER_ID));
            // Ensure the script path is decoded (handles spaces and special characters in the path)
            scriptPath = java.net.URLDecoder.decode(scriptPath, StandardCharsets.UTF_8);
            Process process = this.getProcess(scriptType, scriptPath, params);
            // Capture the script's output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            // Wait for the process to complete
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Python script execution failed with exit code: {}. Output: {}", exitCode, output);
                throw new InternalException("Failed to execute Python script. Output: " + output);
            }
            log.debug("Python script execution completed for user: {}", params.get(PARAM_USER_ID));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Python script execution was interrupted", e);
            throw new InternalException("Python script execution was interrupted: " + scriptPath);
        } catch (Exception e) {
            log.error("Error executing Python script: {}", e.getMessage(), e);
            throw new InternalException("An error occurred while executing the python script: " + scriptPath);
        }
        return output.toString();
    }

    private Process getProcess(SCRIPT_TYPE scriptType, String scriptPath, Map<String, String> params) throws IOException {
        ProcessBuilder processBuilder = switch (scriptType) {
            case RENEW_TOKENS -> new ProcessBuilder(
                    pythonPath, scriptPath,
                    PARAM_NAME, params.get(PARAM_NAME),
                    PARAM_USER_ID, params.get(PARAM_USER_ID),
                    PARAM_PASSWORD, params.get(PARAM_PASSWORD),
                    PARAM_TOTP_KEY, params.get(PARAM_TOTP_KEY),
                    PARAM_API_KEY, params.get(PARAM_API_KEY),
                    PARAM_API_SECRET, params.get(PARAM_API_SECRET),
                    PARAM_REQUEST_TOKEN_URL, params.get(PARAM_REQUEST_TOKEN_URL)
            );
            case GET_ENC_TOKEN -> new ProcessBuilder(
                    pythonPath, scriptPath,
                    PARAM_USER_ID, params.get(PARAM_USER_ID),
                    PARAM_PASSWORD, params.get(PARAM_PASSWORD),
                    PARAM_TOTP_KEY, params.get(PARAM_TOTP_KEY)
            );
            case GET_TOTP -> new ProcessBuilder(
                    pythonPath, scriptPath,
                    PARAM_TOTP_KEY, params.get(PARAM_TOTP_KEY)
            );
        };
        // Set the working directory to the script's location
        processBuilder.directory(new File(scriptPath).getParentFile());
        // Redirect error stream to capture errors
        processBuilder.redirectErrorStream(true);
        return processBuilder.start();
    }

    private String getScriptFromResources(String resourcePath) {
        String scriptPath = null;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) throw new IOException("Resource not found: " + resourcePath);
            File tempScript = File.createTempFile("script", ".py");
            tempScript.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(tempScript)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            scriptPath = tempScript.getAbsolutePath();
        } catch (IOException e) {
            log.error("Error loading script {} from resources: {}", resourcePath, e.getMessage(), e);
        }
        return scriptPath;
    }
}
