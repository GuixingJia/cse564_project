package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.CameraData;
import org.springframework.stereotype.Service;

import java.util.Optional;

/*
 * CameraDataCollectorService
 *
 * Validates and forwards incoming camera frames. This component simulates
 * basic preprocessing by accepting frames only when non-null and containing
 * a non-empty byte array. No image transformation is performed in this mock
 * implementation, but the service provides a clear extension point for future
 * frame preprocessing.
 */
@Service
public class CameraDataCollectorService {

    /*
     * Validates the raw camera frame and returns it if accepted.
     * Returns Optional.empty() when the frame is null or contains no image bytes.
     */
    public Optional<CameraData> processCameraFrame(CameraData rawFrame) {
        if (rawFrame == null) {
            return Optional.empty();
        }

        byte[] imageBytes = rawFrame.getImageBytes();

        // Reject if no image data is present
        if (imageBytes == null || imageBytes.length == 0) {
            return Optional.empty();
        }

        // In a full implementation, image preprocessing could be applied here
        return Optional.of(rawFrame);
    }
}
