package com.fam.vest.service.implementation;
import com.fam.vest.pojo.email.ResendEmailPayload;
import com.fam.vest.service.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IResendEmailService implements EmailService {

    @Value("${resend.api.url}")
    private String apiUrl;

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${resend.sender.email}")
    private String senderEmail;

    @Value("${resend.api.rate.limit.email.interval.seconds}")
    private int emailSendIntervalSeconds;

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, ResendEmailPayload> redisTemplate;
    private ScheduledExecutorService emailSenderExecutor;

    private static final String EMAIL_QUEUE_KEY = "fam-vest-email:queue";

    @Override
    public void sendEmail(ResendEmailPayload resendEmailPayload) {
        // Enqueue the email payload for rate-limited sending in Redis
        log.info("Enqueuing email to Redis queue. Subject: {} To: {}", resendEmailPayload.getSubject(), resendEmailPayload.getTo());
        redisTemplate.opsForList().leftPush(EMAIL_QUEUE_KEY, resendEmailPayload);
    }

    @PostConstruct
    public void startEmailSender() {
        emailSenderExecutor = Executors.newSingleThreadScheduledExecutor();
        emailSenderExecutor.scheduleWithFixedDelay(() -> {
            ResendEmailPayload payload = redisTemplate.opsForList().rightPop(EMAIL_QUEUE_KEY);
            if (payload != null) {
                try {
                    log.info("Dequeued email from Redis queue. Subject: {} To: {}", payload.getSubject(), payload.getTo());
                    this.sendRateLimitedEmail(payload);
                } catch (Exception ex) {
                    log.error("Error sending email from Redis queue", ex);
                }
            }
        }, 0, emailSendIntervalSeconds, TimeUnit.SECONDS);
    }

    private void sendRateLimitedEmail(ResendEmailPayload resendEmailPayload) {
        try {
            // 1. Set up HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey); // Use "Bearer <API_KEY>" automatically

            // 2. Assign sender
            resendEmailPayload.setFrom(senderEmail);

            HttpEntity<ResendEmailPayload> request = new HttpEntity<>(resendEmailPayload, headers);

            // 3. Log before sending
            log.info("Sending email using Resend API: {}", apiUrl);
            log.debug("Payload: {}", resendEmailPayload);

            // 4. Send the POST request
            String response = restTemplate.postForObject(apiUrl, request, String.class);

            // 5. Log success
            log.info("Email sent successfully. Response: {}", response);

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            // Handle HTTP-specific exceptions
            log.error("HTTP error occurred while sending email. Status: {}, Response: {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            // Handle general exceptions
            log.error("Unexpected exception occurred while sending email in ResendEmailService", ex);
        }
    }

    @PreDestroy
    public void stopEmailSender() {
        if (emailSenderExecutor != null) {
            emailSenderExecutor.shutdown();
        }
    }
}
