package com.fam.vest.service.implementation;

import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.exception.ValidationException;
import com.fam.vest.dto.auth.AuthRequest;
import com.fam.vest.dto.auth.SignupRequest;
import com.fam.vest.pojo.email.ResendEmailPayload;
import com.fam.vest.repository.ApplicationUserRepository;
import com.fam.vest.service.EmailService;
import com.fam.vest.service.SignupService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ISignupService implements SignupService {

    private final EmailService emailService;
    private final ApplicationUserRepository applicationUserRepository;
    private final TemplateEngine templateEngine;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String SIGNUP_OTP_CACHE_KEY_PREFIX = "signup:otp:{email}";
    private static final long SIGNUP_OTP_CACHE_TTL = 5; // mins

    @Value("${fam.vest.app.domain}")
    private String applicationDomain;

    @Autowired
    public ISignupService(EmailService emailService,
                          ApplicationUserRepository applicationUserRepository,
                          TemplateEngine templateEngine,
                          StringRedisTemplate redisTemplate,
                          PasswordEncoder passwordEncoder) {
        this.emailService = emailService;
        this.applicationUserRepository = applicationUserRepository;
        this.templateEngine = templateEngine;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void sendLoginOtp(AuthRequest authRequest) {
        ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(authRequest.getEmail());
        if(null == applicationUser) {
            log.error("User does not exist with this email. Email: {}", authRequest.getEmail());
            throw new ValidationException("User does not exist with this email. Please use a different email address or try the \"Create an Account\" option to create your account.");
        }
        String otp = this.getRandomOtp();
        this.sendLoginOtpEmail(authRequest.getEmail(), otp);
        redisTemplate.opsForValue().set(this.getCachedKey(authRequest.getEmail()), otp, SIGNUP_OTP_CACHE_TTL, TimeUnit.MINUTES);
    }

    @Override
    public void initiateSignup(SignupRequest signupRequest) {
        ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(signupRequest.getEmail().toLowerCase());
        if(applicationUser != null) {
            log.error("User already present with this email. Full name: {}, Email: {}", signupRequest.getFullName(), signupRequest.getEmail());
            throw new ValidationException("This email is already registered. Please use a different email address or try the \"Forgot Password?\" option to recover your account.");
        }
        String otp = this.getRandomOtp();
        this.sendSignupOtpEmail(signupRequest.getEmail(), otp);
        redisTemplate.opsForValue().set(this.getCachedKey(signupRequest.getEmail()), otp, SIGNUP_OTP_CACHE_TTL, TimeUnit.MINUTES);
    }

    private String getRandomOtp() {
        SecureRandom secureRandom = new SecureRandom();
        return String.valueOf(secureRandom.nextInt(900000) + 100000);
    }

    private String getCachedKey(String email) {
        return SIGNUP_OTP_CACHE_KEY_PREFIX.replace("{email}", email);
    }
    @Override
    public void verifyOtpAndCreateUser(SignupRequest signupRequest) {

        String expectedOtp = redisTemplate.opsForValue().get(this.getCachedKey(signupRequest.getEmail().toLowerCase()));
        if(StringUtils.isEmpty(expectedOtp)) {
            log.error("The OTP entered seems expired. Please try again or request a new one. Full name: {}, Email: {}", signupRequest.getFullName(), signupRequest.getEmail());
            throw new ValidationException("The OTP you entered seems expired. Please try again or request a new one.");
        }
        if (!signupRequest.getOtp().equals(expectedOtp)) {
            log.error("OTP does not match for user. full name: {}, Email: {}", signupRequest.getFullName(), signupRequest.getEmail());
            throw new ValidationException("Invalid OTP. Please check the OTP and try again.");
        }
        Date currentDate = Calendar.getInstance().getTime();
        ApplicationUser applicationUser = new ApplicationUser();
        applicationUser.setFullName(signupRequest.getFullName());
        applicationUser.setPassword(passwordEncoder.encode(signupRequest.getPassword()));  // <-- HASHED
        applicationUser.setUserName(signupRequest.getEmail().toLowerCase());
        applicationUser.setRole("MEMBER");
        applicationUser.setCreatedBy("System");
        applicationUser.setCreatedDate(currentDate);
        applicationUser.setLastModifiedBy("System");
        applicationUser.setLastModifiedDate(currentDate);
        applicationUserRepository.save(applicationUser);
        redisTemplate.delete(this.getCachedKey(signupRequest.getEmail()));
        this.sendWelcomeEmail(signupRequest);
    }

    @Override
    public boolean validateLoginOtp(AuthRequest authRequest) {
        String expectedOtp = redisTemplate.opsForValue().get(this.getCachedKey(authRequest.getEmail()));
        if(StringUtils.isEmpty(expectedOtp)) {
            log.error("The OTP entered seems expired. Please try again or request a new one. Email: {}", authRequest.getEmail());
            throw new ValidationException("The OTP you entered seems expired. Please try again or request a new one.");
        }
        if (!authRequest.getOtp().equals(expectedOtp)) {
            log.error("OTP does not match for user. Email: {}", authRequest.getEmail());
            throw new ValidationException("Invalid OTP. Please check the OTP and try again.");
        }
        return true;
    }

    private void sendSignupOtpEmail(String email, String otp) {
        ResendEmailPayload resendEmailPayload = new ResendEmailPayload();
        resendEmailPayload.setTo(new String[]{email});
        String subject = "Your OTP for Registration";
        resendEmailPayload.setSubject(subject);
        resendEmailPayload.setHtml(this.buildOtpEmailContent(email, subject, otp));
        emailService.sendEmail(resendEmailPayload);
    }

    private void sendWelcomeEmail(SignupRequest signupRequest) {
        try {
            ResendEmailPayload resendEmailPayload = new ResendEmailPayload();
            resendEmailPayload.setTo(new String[]{signupRequest.getEmail()});
            String subject = "Welcome to FamVest App!";
            resendEmailPayload.setSubject(subject);
            resendEmailPayload.setHtml(this.buildOnboardUserEmailContent(signupRequest, subject));
            emailService.sendEmail(resendEmailPayload);
        } catch(Exception exception) {
            log.error("Error while sending welcome email to user. Full name: {}, Email: {}", signupRequest.getFullName(), signupRequest.getEmail(), exception);
        }
    }

    private void sendLoginOtpEmail(String email, String otp) {
        ResendEmailPayload resendEmailPayload = new ResendEmailPayload();
        resendEmailPayload.setTo(new String[]{email});
        String subject = "Your OTP for Login";
        resendEmailPayload.setSubject(subject);
        resendEmailPayload.setHtml(this.buildOtpEmailContent(email, subject, otp));
        emailService.sendEmail(resendEmailPayload);
    }

    private String buildOtpEmailContent(String email, String subject, String otp) {
        Context contentContext = new Context();
        contentContext.setVariable("name", email);
        contentContext.setVariable("otp", otp);
        String contentHtml = templateEngine.process("email/otp-email", contentContext);

        Context baseContext = new Context();
        baseContext.setVariable("subject", subject);
        baseContext.setVariable("contentHtml", contentHtml);
        return templateEngine.process("email/base-layout", baseContext);
    }

    private String buildOnboardUserEmailContent(SignupRequest signupRequest, String subject) {
        Context contentContext = new Context();
        contentContext.setVariable("fullName", signupRequest.getFullName());
        contentContext.setVariable("appUrl", applicationDomain);
        String contentHtml = templateEngine.process("email/account-created", contentContext);

        Context baseContext = new Context();
        baseContext.setVariable("subject", subject);
        baseContext.setVariable("contentHtml", contentHtml);
        return templateEngine.process("email/base-layout", baseContext);
    }

}
