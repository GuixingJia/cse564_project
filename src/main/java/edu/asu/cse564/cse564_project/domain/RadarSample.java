package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * RadarSample
 *
 * Represents a processed radar measurement produced by the
 * RadarDataCollector. This enriched structure is forwarded to
 * the SpeedViolationController and contains the key attributes
 * needed for speed evaluation and zone-based logic.
 *
 * Includes distance (miles), speed (mph), timestamp, and a
 * simple target identifier used to link measurements together.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadarSample {

    // Vehicle distance from the radar (miles)
    private double distanceMiles;

    // Vehicle speed (mph)
    private double speedMph;

    // Timestamp of radar measurement (ms since epoch)
    private long timestampMillis;

    // Identifier for the tracked vehicle or trajectory
    private long targetId;
}
