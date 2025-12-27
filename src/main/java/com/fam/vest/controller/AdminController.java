package com.fam.vest.controller;

import com.fam.vest.dto.request.StatusUpdateRequest;
import com.fam.vest.dto.response.ApplicationUserDto;
import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.entity.ScheduledTask;
import com.fam.vest.repository.ApplicationUserRepository;
import com.fam.vest.service.AdminService;
import com.fam.vest.service.SchedulerService;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.UserDetailsUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/rest/v1/admin")
@CrossOrigin
public class AdminController {

    private final AdminService adminService;
    private final SchedulerService schedulerService;
    private final ApplicationUserRepository applicationUserRepository;

    @GetMapping("/tools/encrypt")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Object> encryptText(@RequestParam String text) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Encrypting text by admin {}", userDetails.getUsername());
        return CommonUtil.success(adminService.encrypt(text));
    }

    @GetMapping("/schedulers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Object> getScheduleTasks() throws IOException {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching scheduled tasks by admin {}", userDetails.getUsername());
        List<ScheduledTask> scheduledTasks = schedulerService.getAllScheduledTasks();
        return CommonUtil.success(scheduledTasks);
    }

    @GetMapping("/schedulers/{idOrName}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Object> getScheduleTask(@PathVariable String idOrName) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching scheduled task idOrName {} by admin {}", idOrName, userDetails.getUsername());
        ScheduledTask scheduledTask = schedulerService.getScheduledTask(idOrName);
        return CommonUtil.success(scheduledTask);
    }

    @PostMapping("/schedulers/{idOrName}/execute")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Object> executeScheduledTask(@PathVariable String idOrName) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Execute scheduled task idOrName {} by admin {}", idOrName, userDetails.getUsername());
        String response = schedulerService.executeScheduledTask(idOrName);
        return CommonUtil.success(response, response);
    }

    @PatchMapping("/schedulers/{idOrName}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Object> updateScheduledTaskStatus(@PathVariable String idOrName,
                                                               @Valid @RequestBody StatusUpdateRequest request) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Updating scheduled task status for idOrName: {} by admin: {}", idOrName, userDetails.getUsername());
        ScheduledTask scheduledTask = schedulerService.updateScheduledTaskStatus(idOrName, request);
        return CommonUtil.success(scheduledTask, "Scheduled task status updated successfully.");
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Object> getAllApplicationUsers() {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching all application users by admin {}", userDetails.getUsername());
        List<ApplicationUser> allUsers = applicationUserRepository.findAll();
        List<ApplicationUserDto> userDtos = allUsers.stream()
                .sorted(Comparator.comparing(ApplicationUser::getCreatedDate))
                .map(user -> {
                    ApplicationUserDto dto = new ApplicationUserDto();
                    dto.setId(user.getId());
                    dto.setFullName(user.getFullName());
                    dto.setUserName(user.getUserName());
                    dto.setRole(user.getRole());
                    dto.setCreatedAt(user.getCreatedDate());
                    dto.setUpdatedAt(user.getLastModifiedDate());
                     return dto;
                })
                .collect(Collectors.toList());
        return CommonUtil.success(userDtos);
    }

}
