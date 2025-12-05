package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * CameraData
 *
 * Represents a single camera frame captured by the roadside imaging system.
 * Contains raw image bytes and a timestamp. This structure is independent
 * of unit systems and serves as the basic input for ANPR processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraData {

    // Raw image data from the camera sensor
    private byte[] imageBytes;

    // Timestamp of the captured frame (ms since epoch)
    private long timestampMillis;
}
