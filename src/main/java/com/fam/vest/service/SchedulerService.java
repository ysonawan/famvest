package com.fam.vest.service;


import com.fam.vest.dto.request.StatusUpdateRequest;
import com.fam.vest.entity.ScheduledTask;

import java.util.List;

public interface SchedulerService {

    List<ScheduledTask> getAllScheduledTasks();

    ScheduledTask getScheduledTask(String idOrName);

    String executeScheduledTask(String idOrName);

    ScheduledTask updateScheduledTaskStatus(String idOrName, StatusUpdateRequest statusUpdateRequest);
}
