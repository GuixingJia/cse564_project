package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * ViolationRecord
 *
 * Represents the final packaged evidence record produced by the system.
 * This record aggregates all required information for backend upload,
 * including speed, distance, plate number, capture time, target identity,
 * and associated image data. It is the final output of the evidence
 * collection and packaging pipeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationRecord {

    // Locally assigned unique violation ID
    private String violationId;

    // License plate number recognized by ANPR
    private String plateNumber;

    // Vehicle speed at the time of violation (mph)
    private double speedMph;

    // Distance from the device in miles
    private double distanceMiles;

    // Distance from the device in meters
    private double distanceMeters;

    // Timestamp of the violation event (ms since epoch)
    private long timestampMillis;

    // Radar-assigned target identifier
    private long targetId;

    // Captured image data used as evidence
    private byte[] imageBytes;
}
