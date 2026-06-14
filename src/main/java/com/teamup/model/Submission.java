package com.teamup.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a student's submission for a task.
 *
 * The actual PDF file is stored in MongoDB GridFS. This entity holds:
 *   - A reference (gridFsFileId) to the file in MongoDB GridFS
 *   - Metadata about the submission (original filename, submission time)
 *
 * This hybrid approach (PostgreSQL + MongoDB GridFS) gives us:
 *   - Relational integrity via JPA/Hibernate
 *   - Efficient binary storage via GridFS
 */
@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long submissionId;

    /**
     * MongoDB GridFS ObjectId of the stored PDF file.
     * This is the primary reference to the actual file content.
     *
     * Stored as VARCHAR(64) so PostgreSQL can hold the ObjectId string.
     * Nullable only for backwards compatibility — new submissions always have this set.
     */
    @Column(name = "gridfs_file_id", length = 64)
    private String gridFsFileId;

    /**
     * The original filename as uploaded by the student.
     * Displayed in the leader review portal and report exports.
     */
    @NotBlank(message = "Original filename is required")
    @Size(max = 255, message = "Filename must not exceed 255 characters")
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    /**
     * Content type — always "application/pdf" in this project.
     */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /**
     * File size in bytes. Useful for validation and display.
     */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    // --- Relationships ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_submission_task"))
    private Task task;

    // --- Lifecycle Callbacks ---

    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
        if (this.contentType == null) {
            this.contentType = "application/pdf";
        }
    }
}
