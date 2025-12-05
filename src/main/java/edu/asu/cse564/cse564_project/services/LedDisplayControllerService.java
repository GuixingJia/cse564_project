package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.LedCommand;
import edu.asu.cse564.cse564_project.domain.SpeedStatus;
import org.springframework.stereotype.Service;

/*
 * LedDisplayControllerService
 *
 * Translates SpeedStatus into a human-readable LED display command.
 * The LED always shows the current speed, and highlights warnings when
 * the vehicle is overspeeding. This controller does not apply any zone
 * logic; it simply formats the message based on overspeed status.
 */
@Service
public class LedDisplayControllerService {

    /*
     * Builds a LedCommand for the physical LED display.
     * Returns a default "no data" entry if SpeedStatus is null.
     */
    public LedCommand buildLedCommand(SpeedStatus status) {
        if (status == null) {
            // Defensive fallback for missing speed data
            return LedCommand.builder()
                    .speedMph(0.0)
                    .distanceMiles(0.0)
                    .overspeed(false)
                    .message("NO SPEED DATA")
                    .build();
        }

        double speedMph = status.getSpeedMph();
        double distanceMiles = status.getDistanceMiles();
        boolean overspeed = status.isOverspeed();

        String message;
        if (overspeed) {
            // Overspeed warning message
            message = String.format("OVERSPEED: %.1f mph - SLOW DOWN", speedMph);
        } else {
            // Normal speed display
            message = String.format("Speed: %.1f mph - OK", speedMph);
        }

        return LedCommand.builder()
                .speedMph(speedMph)
                .distanceMiles(distanceMiles)
                .overspeed(overspeed)
                .message(message)
                .build();
    }
}
