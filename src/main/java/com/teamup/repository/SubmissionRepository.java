package com.teamup.repository;

import com.teamup.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByTaskTaskIdOrderBySubmittedAtDesc(Long taskId);

    boolean existsByTaskTaskId(Long taskId);
}
