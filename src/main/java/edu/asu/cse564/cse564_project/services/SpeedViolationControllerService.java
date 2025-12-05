package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.RadarSample;
import edu.asu.cse564.cse564_project.domain.SpeedContext;
import edu.asu.cse564.cse564_project.domain.SpeedStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

/*
 * SpeedViolationControllerService
 *
 * Evaluates radar samples to determine overspeed conditions.
 * Always produces SpeedStatus for LED display.
 * Produces SpeedContext only when overspeed occurs within the active monitoring zone.
 * Monitoring zones are defined based on distance in meters.
 */
@Service
public class SpeedViolationControllerService {

    // Base allowed speed in mph
    private static final double SPEED_LIMIT_MPH = 40.0;

    // Overspeed tolerance (10%)
    private static final double TOLERANCE_RATIO = 0.10;

    // Lower boundary for the monitoring zone (meters)
    private static final double MONITOR_ZONE_START_METERS = -90.0;

    private final UnitConversionService unitConversionService;

    public SpeedViolationControllerService(UnitConversionService unitConversionService) {
        this.unitConversionService = unitConversionService;
    }

    /*
     * Build SpeedStatus for LED display.
     * Always produced regardless of distance.
     */
    public SpeedStatus buildSpeedStatus(RadarSample sample) {
        boolean overspeed = isOverspeed(sample.getSpeedMph());
        return SpeedStatus.builder()
                .speedMph(sample.getSpeedMph())
                .distanceMiles(sample.getDistanceMiles())
                .overspeed(overspeed)
                .build();
    }

    /*
     * Build SpeedContext only if:
     *   - speed is overspeed, and
     *   - distanceMeters > MONITOR_ZONE_START_METERS
     */
    public Optional<SpeedContext> buildOverspeedContext(RadarSample sample) {
        boolean overspeed = isOverspeed(sample.getSpeedMph());
        if (!overspeed) {
            return Optional.empty();
        }

        double distanceMiles = sample.getDistanceMiles();
        double distanceMeters = unitConversionService.milesToMeters(distanceMiles);

        // Coarse-only region: do not generate SpeedContext
        if (distanceMeters <= MONITOR_ZONE_START_METERS) {
            return Optional.empty();
        }

        // Construct SpeedContext for ECC and evidence pipeline
        SpeedContext ctx = SpeedContext.builder()
                .overspeed(true)
                .speedMph(sample.getSpeedMph())
                .distanceMiles(distanceMiles)
                .distanceMeters(distanceMeters)
                .timestampMillis(sample.getTimestampMillis())
                .targetId(sample.getTargetId())
                .build();

        return Optional.of(ctx);
    }

    // Determines whether the given speed is above the overspeed threshold.
    private boolean isOverspeed(double speedMph) {
        double threshold = SPEED_LIMIT_MPH * (1.0 + TOLERANCE_RATIO);
        return speedMph >= threshold;
    }
}
