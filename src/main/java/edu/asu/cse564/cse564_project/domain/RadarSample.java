package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enriched radar sample produced by the RadarDataCollector.
 *
 * This corresponds to the event that is forwarded to the
 * SpeedViolationController after basic filtering:
 *   - distance (miles)
 *   - speed (mph)
 *   - timestamp (ms since epoch)
 *   - targetId (simple sequence in this project)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadarSample {

    /**
     * Distance from the radar to the vehicle, in miles.
     */
    private double distanceMiles;

    /**
     * Vehicle speed in miles per hour (mph).
     */
    private double speedMph;

    /**
     * Timestamp in milliseconds since Unix epoch.
     * In this project, we assume clocks are synchronized.
     */
    private long timestampMillis;

    /**
     * Simple monotonically increasing ID used to distinguish
     * different detected vehicles (or trajectories).
     * In a real system this would come from the radar itself,
     * but here we approximate in software.
     */
    private long targetId;
}
