package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Raw data produced by the roadside Radar/LiDAR sensor.
 * All physical units in this system follow U.S. customary units:
 *  - distance: miles
 *  - speed: miles per hour (mph)
 * This corresponds to:
 *    event(RadarData) radar_data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadarData {

    /*
     * Longitudinal distance from the radar to the vehicle, in miles.
     * Positive values: vehicle is in front of the radar.
     */
    private double distanceMiles;

    /*
     * Vehicle speed as measured by the radar, in miles per hour (mph).
     */
    private double speedMph;
}
