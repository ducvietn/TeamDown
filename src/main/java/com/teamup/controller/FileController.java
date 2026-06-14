package com.teamup.controller;

import com.teamup.dto.ApiResponse;
import com.teamup.dto.SubmissionResponse;
import com.teamup.service.GridFSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;

/**
 * Handles PDF file operations via MongoDB GridFS.
 *
 * Architecture:
 *   Student uploads PDF → FileController → GridFSService → MongoDB GridFS
 *                                        ↓
 *                         GridFS ObjectId stored in PostgreSQL Submission record
 *
 *   Leader downloads PDF → FileController → GridFSService → MongoDB GridFS
 *                          ← GridFS ObjectId from PostgreSQL Submission record
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class FileController {

    private final GridFSService gridFSService;

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    // ─────────────────────────────────────────────────────────────
    // Upload a PDF file (multipart/form-data)
    // ─────────────────────────────────────────────────────────────

    /**
     * POST /api/files/upload
     *
     * Uploads a PDF file to MongoDB GridFS.
     *
     * Request (multipart/form-data):
     *   - file       : MultipartFile — the PDF to store
     *   - uploaderId : String — user ID of the student
     *   - taskId     : String — task ID this submission belongs to
     *   - groupId    : String (optional) — group ID for metadata
     *
     * Response:
     *   - gridFsFileId    : String — the MongoDB ObjectId (store this in Submission.gridFsFileId)
     *   - originalFilename: String — the student's original filename
     *   - fileSizeBytes  : Long   — file size in bytes
     *   - contentType    : String — "application/pdf"
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploaderId") String uploaderId,
            @RequestParam("taskId") String taskId,
            @RequestParam(value = "groupId", required = false) String groupId) {

        log.info("File upload request: uploader={}, task={}, filename={}, size={}",
                uploaderId, taskId, file.getOriginalFilename(), file.getSize());

        ObjectId gridFsId = gridFSService.uploadFile(file, uploaderId, taskId);

        FileUploadResponse response = FileUploadResponse.builder()
                .gridFsFileId(gridFsId.toHexString())
                .originalFilename(file.getOriginalFilename())
                .fileSizeBytes(file.getSize())
                .contentType(PDF_CONTENT_TYPE)
                .build();

        log.info("File uploaded successfully: gridFsId={}", gridFsId.toHexString());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("File uploaded to MongoDB GridFS", response));
    }

    // ─────────────────────────────────────────────────────────────
    // Download a PDF file
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/files/{gridFsFileId}
     *
     * Streams a PDF file from MongoDB GridFS to the client.
     *
     * Usage: Used by the leader review portal and the report export
     *        to preview/download submitted PDFs.
     *
     * The gridFsFileId comes from Submission.gridFsFileId (PostgreSQL).
     */
    @GetMapping("/{gridFsFileId}")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable String gridFsFileId) {

        log.info("File download request: gridFsFileId={}", gridFsFileId);

        ObjectId objectId;
        try {
            objectId = new ObjectId(gridFsFileId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid GridFS ObjectId format: {}", gridFsFileId);
            return ResponseEntity.badRequest().build();
        }

        // Get original filename from metadata for the Content-Disposition header
        String originalFilename = gridFSService
                .getOriginalFilename(objectId)
                .orElse("submission.pdf");

        try (InputStream in = gridFSService.getFileStream(objectId)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    sanitizeFilename(originalFilename));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(in));

        } catch (Exception e) {
            log.error("Failed to stream file: gridFsFileId={}", gridFsFileId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * HEAD /api/files/{gridFsFileId}
     *
     * Checks if a file exists in GridFS without downloading it.
     * Useful for frontend validation before rendering a download button.
     */
    @RequestMapping(value = "/{gridFsFileId}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkFileExists(@PathVariable String gridFsFileId) {
        try {
            ObjectId objectId = new ObjectId(gridFsFileId);
            Optional<GridFsResource> resource = gridFSService.getFile(objectId);
            if (resource.isPresent()) {
                return ResponseEntity.ok().build();
            }
        } catch (Exception _) {}
        return ResponseEntity.notFound().build();
    }

    // ─────────────────────────────────────────────────────────────
    // Delete a file
    // ─────────────────────────────────────────────────────────────

    /**
     * DELETE /api/files/{gridFsFileId}
     *
     * Deletes a file from GridFS. Called when a submission is rejected
     * and the student needs to re-upload.
     */
    @DeleteMapping("/{gridFsFileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String gridFsFileId) {
        try {
            ObjectId objectId = new ObjectId(gridFsFileId);
            boolean deleted = gridFSService.deleteFile(objectId);
            if (deleted) {
                log.info("File deleted from GridFS: gridFsFileId={}", gridFsFileId);
                return ResponseEntity.ok(ApiResponse.ok("File deleted", null));
            }
        } catch (Exception e) {
            log.error("Failed to delete file: gridFsFileId={}", gridFsFileId, e);
        }
        return ResponseEntity.notFound().build();
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private String sanitizeFilename(String filename) {
        if (filename == null) return "submission.pdf";
        // Strip any path components and ensure .pdf extension
        String name = filename.replaceAll("[/\\\\:]", "_");
        if (!name.toLowerCase().endsWith(".pdf")) {
            name += ".pdf";
        }
        return name;
    }

    // ─────────────────────────────────────────────────────────────
    // Inner DTO for upload response
    // ─────────────────────────────────────────────────────────────

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    private static class FileUploadResponse {
        private String gridFsFileId;
        private String originalFilename;
        private Long fileSizeBytes;
        private String contentType;
    }
}
