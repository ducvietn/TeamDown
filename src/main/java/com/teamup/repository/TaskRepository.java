package com.teamup.repository;

import com.teamup.model.Task;
import com.teamup.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByGroupGroupId(Long groupId);

    List<Task> findByAssignedToUserId(Long userId);

    List<Task> findByStatus(TaskStatus status);

    @Query("""
        SELECT t FROM Task t
        JOIN FETCH t.group g
        LEFT JOIN FETCH t.assignedTo
        WHERE t.group.groupId = :groupId
        ORDER BY t.createdAt DESC
        """)
    List<Task> findByGroupIdWithDetails(@Param("groupId") Long groupId);

    @Query("""
        SELECT t FROM Task t
        WHERE t.status = :status
          AND t.lastProgressUpdate < :threshold
        """)
    List<Task> findStaleInProgressTasks(
        @Param("status") TaskStatus status,
        @Param("threshold") LocalDateTime threshold
    );

    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.submissions
        WHERE t.taskId = :taskId
        """)
    Task findByIdWithSubmissions(@Param("taskId") Long taskId);
}
