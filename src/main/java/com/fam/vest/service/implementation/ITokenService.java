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

@Slf4j
@Service
public class ITokenService implements TokenService {

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
        List<TradingAccount> tradingAccounts = null;
        if(isOnApplicationStart) {
            tradingAccounts = tradingAccountRepository.findAllByOrderByIdAsc();
        } else {
            ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userDetails.getUsername());
            return applicationUserTradingAccountMappingRepository.findTradingAccountsByApplicationUserId(applicationUser.getId());
        }
        return tradingAccounts;
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
        tradingAccounts.forEach(tradingAccount -> {
            if(null != tradingAccount) {
                this.runScriptToRenewRequestToken(tradingAccount);
            }
        });
        return "Request token renewal completed for all users. Please verify the status on dashboard.";
    }

    private void runScriptToRenewRequestToken(TradingAccount tradingAccount) {
        if(StringUtils.isNotBlank(tradingAccount.getPassword()) && StringUtils.isNotBlank(tradingAccount.getTotpKey())) {
            Map<String, String> params = new HashMap<>();
            params.put("--name", tradingAccount.getName());
            params.put("--user_id", tradingAccount.getUserId());
            params.put("--password", tradingAccount.getPassword());
            params.put("--totp_key", tradingAccount.getTotpKey());
            params.put("--api_key", tradingAccount.getApiKey());
            params.put("--api_secret", tradingAccount.getApiSecret());
            params.put("--request_token_url", kiteLoginEndpoint);
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
            params.put("--user_id", tradingAccount.getUserId());
            params.put("--password", tradingAccount.getPassword());
            params.put("--totp_key", tradingAccount.getTotpKey());
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
        params.put("--totp_key", tradingAccount.getTotpKey());
        params.put("--user_id", tradingAccount.getUserId());
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
            log.debug("Running python script for user: {}", params.get("--user_id"));
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
            log.debug("Python script execution completed for user: {}", params.get("--user_id"));
        } catch (Exception e) {
            log.error("Error executing Python script: {}", e.getMessage(), e);
            throw new InternalException("An error occurred while executing the python script: "+scriptPath);
        }
        return output.toString();
    }

    private Process getProcess(SCRIPT_TYPE scriptType, String scriptPath, Map<String, String> params) throws IOException {
        ProcessBuilder processBuilder = switch (scriptType) {
            case RENEW_TOKENS -> new ProcessBuilder(
                    pythonPath, scriptPath,
                    "--name", params.get("--name"),
                    "--user_id", params.get("--user_id"),
                    "--password", params.get("--password"),
                    "--totp_key", params.get("--totp_key"),
                    "--api_key", params.get("--api_key"),
                    "--api_secret", params.get("--api_secret"),
                    "--request_token_url", params.get("--request_token_url")
            );
            case GET_ENC_TOKEN -> new ProcessBuilder(
                    pythonPath, scriptPath,
                    "--user_id", params.get("--user_id"),
                    "--password", params.get("--password"),
                    "--totp_key", params.get("--totp_key")
            );
            case GET_TOTP -> new ProcessBuilder(
                    pythonPath, scriptPath,
                    "--totp_key", params.get("--totp_key")
            );
            default -> throw new IllegalArgumentException("Invalid script type: " + scriptType);
        };
        // Set the working directory to the script's location
        processBuilder.directory(new File(scriptPath).getParentFile());
        // Redirect error stream to capture errors
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        return process;
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
