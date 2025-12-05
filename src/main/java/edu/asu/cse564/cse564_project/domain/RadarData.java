package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * RadarData
 *
 * Represents raw measurements produced by a roadside radar or LiDAR
 * sensor. All values are reported in U.S. customary units (miles, mph).
 * This structure is the initial input to the RadarDataCollector.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadarData {

    // Longitudinal distance from radar to vehicle (miles)
    private double distanceMiles;

    // Vehicle speed measured by the radar (mph)
    private double speedMph;
}
