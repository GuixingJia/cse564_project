package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.UploadStatus;
import edu.asu.cse564.cse564_project.domain.ViolationRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/*
 * BackendUplinkControllerService
 *
 * Simulates uploading violation records to a central backend system.
 * This mock implementation stores records in an in-memory buffer and
 * generates a fake backendRecordId. In a real deployment, this service
 * would make network calls, handle retries, and return backend responses.
 */
@Service
public class BackendUplinkControllerService {

    // Local in-memory buffer simulating a persistent upload queue
    private final List<ViolationRecord> localBuffer =
            Collections.synchronizedList(new ArrayList<>());

    /*
     * Simulates uploading a violation record to a backend system.
     * Returns an UploadStatus describing the outcome.
     */
    public UploadStatus uploadViolationRecord(ViolationRecord record) {
        long now = System.currentTimeMillis();

        // Reject null input
        if (record == null) {
            return UploadStatus.builder()
                    .success(false)
                    .retryCount(0)
                    .backendRecordId(null)
                    .message("ViolationRecord is null, nothing to upload.")
                    .timestampMillis(now)
                    .build();
        }

        // Generate a fake backend record ID (simulated success)
        String backendRecordId = UUID.randomUUID().toString();

        // Store the record in the local buffer
        localBuffer.add(record);

        return UploadStatus.builder()
                .success(true)
                .retryCount(0)
                .backendRecordId(backendRecordId)
                .message("ViolationRecord uploaded (simulated) successfully.")
                .timestampMillis(now)
                .build();
    }

    /*
     * Returns a snapshot of all locally stored violation records.
     * Useful for debugging and monitoring.
     */
    public List<ViolationRecord> getBufferedRecordsSnapshot() {
        synchronized (localBuffer) {
            return new ArrayList<>(localBuffer);
        }
    }

    /*
     * Clears the local buffer. Used for testing or resetting system state.
     */
    public void clearBuffer() {
        localBuffer.clear();
    }
}
