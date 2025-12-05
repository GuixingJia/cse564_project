package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * LedCommand
 *
 * Represents the formatted output sent from the LEDDisplayController
 * to the physical roadside LED display. It carries both raw values
 * (speed, distance, overspeed status) and the final message string
 * that will be shown to the driver.
 *
 * Used for visual feedback such as "Speed: 35 mph - OK" or
 * "OVERSPEED: 52 mph - SLOW DOWN".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedCommand {

    // Current vehicle speed (mph)
    private double speedMph;

    // Current distance from radar (miles)
    private double distanceMiles;

    // Whether the vehicle is overspeed
    private boolean overspeed;

    // Final text message displayed on the LED screen
    private String message;
}
