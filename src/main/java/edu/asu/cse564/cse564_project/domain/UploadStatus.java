package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * UploadStatus
 *
 * Represents the result of attempting to upload a ViolationRecord to the
 * central backend system. Used by the BackendUplinkController to report
 * success, generated backend record ID, retry attempts, and a debug message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadStatus {

    // Indicates whether the upload operation was successful
    private boolean success;

    // Backend-assigned identifier for the stored violation (if any)
    private String backendRecordId;

    // Number of retry attempts performed before the final result
    private int retryCount;

    // Human-readable diagnostic or status message
    private String message;

    // Timestamp of when the upload attempt completed (ms since epoch)
    private long timestampMillis;
}
