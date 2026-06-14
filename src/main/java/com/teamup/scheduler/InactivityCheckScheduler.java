package com.teamup.scheduler;

import com.teamup.model.Task;
import com.teamup.model.TaskStatus;
import com.teamup.repository.TaskRepository;
import com.teamup.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InactivityCheckScheduler {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    /**
     * Runs every 24 hours at midnight.
     * Cron: second minute hour day-of-month month day-of-week
     * Default: "0 0 0 * * *" — at 00:00:00 every day
     *
     * Logic:
     *   1. Find all tasks with status = IN_PROGRESS
     *   2. For each task, check if lastProgressUpdate is older than 72 hours (3 days)
     *   3. If yes → send an inactivity warning notification to the assignee
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void checkInactivity() {
        log.info("=== Inactivity check started ===");
        LocalDateTime threshold = LocalDateTime.now().minusHours(72);

        List<Task> staleTasks = taskRepository.findStaleInProgressTasks(
                TaskStatus.IN_PROGRESS, threshold
        );

        log.info("Found {} tasks with no progress update for 72+ hours", staleTasks.size());

        int sent = 0;
        for (Task task : staleTasks) {
            if (task.getAssignedTo() != null) {
                notificationService.sendInactivityWarning(task, task.getAssignedTo());
                sent++;
            }
        }

        log.info("=== Inactivity check completed: {} warning(s) sent ===", sent);
    }
}
