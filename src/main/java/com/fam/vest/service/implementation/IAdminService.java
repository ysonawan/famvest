package com.fam.vest.service.implementation;

import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.entity.ScheduledTask;
import com.fam.vest.entity.converter.EncryptionUtils;
import com.fam.vest.pojo.email.ResendEmailPayload;
import com.fam.vest.repository.ApplicationUserRepository;
import com.fam.vest.repository.ScheduledTaskRepository;
import com.fam.vest.service.*;
import com.fam.vest.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class IAdminService implements AdminService {

    private final SnapshotService snapshotService;
    private final EncryptionUtils encryptionUtils;
    private final ApplicationUserRepository applicationUserRepository;
    private final ScheduledTaskRepository scheduledTaskRepository;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    @Override
    public void captureSnapshot() {
        snapshotService.captureSnapshot();
    }

    @Override
    public String encrypt(String text) {
        return encryptionUtils.encrypt(text);
    }

    @Override
    public void restartApplication() {
        log.info("Restarting application");
        System.exit(0);
    }

    @Override
    public void notifySchedulerErrors() {
        List<ApplicationUser> applicationUsers = applicationUserRepository.findApplicationUserByRole("ADMIN");
        if (applicationUsers.isEmpty()) {
            log.warn("No admin users found to notify about scheduler errors");
            return;
        }
        List<ScheduledTask> scheduledTasks = scheduledTaskRepository.findAll();
        List<ScheduledTask> failedScheduledTasks = scheduledTasks.stream().filter(task -> task.getStatus() != null && task.getStatus().equals("FAILED")).toList();
        if(!failedScheduledTasks.isEmpty()) {
            String subject = "Scheduler Error Notification - " + CommonUtil.formatDate(LocalDate.now());
            String [] to = applicationUsers.stream().map(ApplicationUser::getUserName).toArray(String[]::new);
            ResendEmailPayload resendEmailPayload = new ResendEmailPayload();
            resendEmailPayload.setTo(to);
            resendEmailPayload.setSubject(subject);
            resendEmailPayload.setHtml(this.getEmailBody(subject, failedScheduledTasks));
            emailService.sendEmail(resendEmailPayload);
        } else {
            log.info("No failed scheduled tasks found to notify. No email sent.");
        }
    }

    private String getEmailBody(String subject, List<ScheduledTask> scheduledTasks) {
        // Create inner content context
        Context reportContext = new Context();
        reportContext.setVariable("scheduledTasks", scheduledTasks);
        reportContext.setVariable("subject", subject);

        // Process the report template
        String contentHtml = templateEngine.process("email/scheduler-error-notification.html", reportContext);

        // Wrap in base layout
        Context baseContext = new Context();
        baseContext.setVariable("subject", subject);
        baseContext.setVariable("contentHtml", contentHtml);

        return templateEngine.process("email/base-layout", baseContext);
    }
}
