package com.teamup.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class SubmissionResponse {

    private Long submissionId;

    /** MongoDB GridFS ObjectId — use this to download the file */
    private String gridFsFileId;

    /** Original filename as the student uploaded it */
    private String originalFilename;

    /** Content type (always application/pdf) */
    private String contentType;

    /** File size in bytes */
    private Long fileSizeBytes;

    private String submittedAt;
    private Long taskId;
    private String taskName;
}
