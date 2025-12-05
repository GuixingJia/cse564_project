package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * SpeedStatus
 *
 * Represents real-time speed information to be consumed by the
 * LEDDisplayController. This event is always generated regardless
 * of monitoring zone and provides the current speed, distance, and
 * overspeed status for visual feedback to drivers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeedStatus {

    // Current vehicle speed (mph)
    private double speedMph;

    // Current longitudinal distance from the radar unit (miles)
    private double distanceMiles;

    // Whether the measured speed qualifies as overspeed
    private boolean overspeed;
}
