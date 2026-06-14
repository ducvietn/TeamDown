package com.teamup.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.gridfs.GridFsUploadFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Service layer for MongoDB GridFS file operations.
 *
 * GridFS stores large files (like PDF submissions) as chunks in MongoDB,
 * avoiding the need for a separate file storage service (S3, GCS, etc.).
 *
 * Files are stored with rich metadata for traceability:
 *   - originalFilename  — the student's original file name
 *   - contentType       — MIME type (application/pdf)
 *   - uploaderId        — which user uploaded the file
 *   - taskId            — which task this submission belongs to
 *   - groupId           — which group the task belongs to
 *   - uploadTimestamp   — when the file was uploaded
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GridFSService {

    private final GridFsTemplate gridFsTemplate;
    private final GridFSBucket gridFSBucket;

    // ─────────────────────────────────────────────────────────────
    // Upload a file
    // ─────────────────────────────────────────────────────────────

    /**
     * Stores a file in GridFS with rich metadata.
     *
     * @param file     The multipart file to store (must be a PDF)
     * @param metadata Additional metadata (uploaderId, taskId, groupId, ...)
     * @return The ObjectId of the stored file — this is what goes into
     *         the Submission.gridFsFileId field in PostgreSQL
     */
    public ObjectId uploadFile(MultipartFile file, Document metadata) {
        validateFile(file);

        GridFSUploadOptions options = new GridFSUploadOptions()
                .metadata(metadata);

        try (InputStream in = file.getInputStream()) {
            ObjectId fileId = gridFsTemplate.store(
                    in,
                    sanitizeFileName(file.getOriginalFilename()),
                    file.getContentType(),
                    options
            );
            log.info("File stored in GridFS: id={}, name={}, size={} bytes, uploader={}",
                    fileId,
                    file.getOriginalFilename(),
                    file.getSize(),
                    metadata.get("uploaderId"));
            return fileId;
        } catch (IOException e) {
            log.error("Failed to upload file to GridFS: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience overload — stores a file with minimal metadata.
     */
    public ObjectId uploadFile(MultipartFile file, String uploaderId, String taskId) {
        Document metadata = new Document()
                .append("uploaderId", uploaderId)
                .append("taskId", taskId)
                .append("uploadTimestamp", java.time.Instant.now().toString())
                .append("originalFilename", file.getOriginalFilename())
                .append("contentType", file.getContentType())
                .append("sizeBytes", file.getSize());
        return uploadFile(file, metadata);
    }

    // ─────────────────────────────────────────────────────────────
    // Download / retrieve files
    // ─────────────────────────────────────────────────────────────

    /**
     * Retrieves a file from GridFS by its ObjectId.
     * Returns null if the file does not exist.
     */
    public Optional<GridFsResource> getFile(ObjectId fileId) {
        GridFSFile file = gridFsTemplate.findOne(
                new Query(Criteria.where("_id").is(fileId))
        );
        if (file == null) {
            log.warn("GridFS file not found: id={}", fileId);
            return Optional.empty();
        }
        return Optional.of(gridFsTemplate.getResource(file));
    }

    /**
     * Retrieves a file and streams its content directly to an OutputStream.
     * Preferred for large PDFs — avoids loading the entire file into memory.
     */
    public void downloadFile(ObjectId fileId, OutputStream out) {
        try {
            gridFSBucket.downloadToStream(fileId, out);
        } catch (Exception e) {
            log.error("Failed to download GridFS file: id={}", fileId, e);
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }

    /**
     * Streams a file from GridFS using a MongoDB DownloadStream.
     * Memory-efficient for very large files.
     */
    public InputStream getFileStream(ObjectId fileId) {
        return gridFSBucket.openDownloadStream(fileId);
    }

    // ─────────────────────────────────────────────────────────────
    // Metadata
    // ─────────────────────────────────────────────────────────────

    /**
     * Retrieves the GridFS file metadata document.
     */
    public Optional<Document> getFileMetadata(ObjectId fileId) {
        GridFSFile file = gridFsTemplate.findOne(
                new Query(Criteria.where("_id").is(fileId))
        );
        return Optional.ofNullable(file != null ? file.getMetadata() : null);
    }

    /**
     * Retrieves the original filename from GridFS metadata.
     */
    public Optional<String> getOriginalFilename(ObjectId fileId) {
        return getFileMetadata(fileId)
                .map(meta -> meta.getString("originalFilename"));
    }

    // ─────────────────────────────────────────────────────────────
    // Delete
    // ─────────────────────────────────────────────────────────────

    /**
     * Deletes a file from GridFS by its ObjectId.
     */
    public boolean deleteFile(ObjectId fileId) {
        try {
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(fileId)));
            log.info("GridFS file deleted: id={}", fileId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete GridFS file: id={}", fileId, e);
            return false;
        }
    }

    /**
     * Deletes all files associated with a given taskId.
     * Useful when a task is permanently deleted.
     */
    public long deleteFilesByTaskId(String taskId) {
        var query = new Query(Criteria.where("metadata.taskId").is(taskId));
        var files = gridFsTemplate.find(query);
        long count = 0;
        for (GridFSFile file : files) {
            deleteFile(file.getObjectId());
            count++;
        }
        log.info("Deleted {} GridFS files for taskId={}", count, taskId);
        return count;
    }

    // ─────────────────────────────────────────────────────────────
    // Validation helpers
    // ─────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or missing.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException(
                    "Only PDF files are accepted. Received: " + contentType);
        }
        // 50 MB limit
        long maxSize = 50L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    "File exceeds the maximum allowed size of 50 MB.");
        }
    }

    private String sanitizeFileName(String filename) {
        if (filename == null) return "unnamed";
        // Remove path traversal characters for safety
        return filename.replaceAll("[/\\\\:]", "_");
    }
}
