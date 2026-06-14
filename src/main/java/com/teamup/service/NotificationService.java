package com.teamup.service;

import com.teamup.model.Notification;
import com.teamup.model.Task;
import com.teamup.model.User;
import com.teamup.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private static final String TYPE_INACTIVITY_WARNING = "INACTIVITY_WARNING";
    private static final String TYPE_TASK_APPROVED = "TASK_APPROVED";
    private static final String TYPE_TASK_REJECTED = "TASK_REJECTED";
    private static final String TYPE_SUBMISSION_PENDING = "SUBMISSION_PENDING";

    @Transactional
    public void sendInactivityWarning(Task task, User assignee) {
        if (assignee == null) {
            log.warn("Cannot send inactivity warning: assignee is null for task {}", task.getTaskId());
            return;
        }

        String message = String.format(
            "Cảnh báo: Tiến độ công việc [%s] của bạn đang bị đóng băng 3 ngày qua. Vui lòng cập nhật!",
            task.getTaskName()
        );

        Notification notification = Notification.builder()
                .message(message)
                .type(TYPE_INACTIVITY_WARNING)
                .user(assignee)
                .task(task)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Inactivity warning sent to user {} for task {}",
                assignee.getUserId(), task.getTaskId());
    }

    @Transactional
    public void sendTaskApproved(Task task, User assignee) {
        if (assignee == null) return;

        String message = String.format(
            "Công việc [%s] đã được Trưởng nhóm phê duyệt và hoàn thành.",
            task.getTaskName()
        );

        Notification notification = Notification.builder()
                .message(message)
                .type(TYPE_TASK_APPROVED)
                .user(assignee)
                .task(task)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    @Transactional
    public void sendTaskRejected(Task task, User assignee) {
        if (assignee == null) return;

        String message = String.format(
            "Công việc [%s] đã bị Trưởng nhóm từ chối. Vui lòng cập nhật lại tiến độ!",
            task.getTaskName()
        );

        Notification notification = Notification.builder()
                .message(message)
                .type(TYPE_TASK_REJECTED)
                .user(assignee)
                .task(task)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    public List<Notification> getUnreadByUser(Long userId) {
        return notificationRepository.findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getAllByUser(Long userId) {
        return notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository
                .findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }
}
