package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * SpeedContext
 *
 * Represents contextual information for an overspeed event.
 * This object is forwarded to the EvidenceCaptureController
 * and EvidenceCollector when the monitored vehicle exceeds
 * the defined speed threshold and is within the monitoring zone.
 *
 * Contains speed, distance (miles and meters), timestamp, and
 * target identity for correlating radar and image evidence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeedContext {

    // Whether the sample is classified as overspeed
    private boolean overspeed;

    // Vehicle speed (mph)
    private double speedMph;

    // Distance from radar in miles
    private double distanceMiles;

    // Distance from radar in meters
    private double distanceMeters;

    // Timestamp of the radar measurement (ms since epoch)
    private long timestampMillis;

    // Target identifier assigned by radar tracking
    private long targetId;
}
