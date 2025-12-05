package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.RadarData;
import edu.asu.cse564.cse564_project.domain.RadarSample;
import org.springframework.stereotype.Service;

import java.util.Optional;

/*
 * RadarDataCollectorService
 *
 * Filters and normalizes raw radar measurements before they enter
 * the CPS pipeline. Uses distance in meters to classify the vehicle
 * into different zones (out-of-range, active monitoring, leaving),
 * while preserving the original distance in miles for downstream use.
 *
 * Distance zones (meters):
 *   d <= -150m        : OUT_OF_RANGE_BEFORE  → discard
 *   -150m < d <= 20m  : ACTIVE_MONITOR_ZONE  → always forward samples
 *   20m < d <= 90m    : LEAVING_ZONE         → forward only the first sample crossing > 20m
 *   d > 90m           : OUT_OF_RANGE_AFTER   → discard and reset state
 *
 * This implementation assumes a single tracked vehicle whose distance
 * increases monotonically as it passes the device.
 */
@Service
public class RadarDataCollectorService {

    // Minimum valid distance in meters (upstream boundary)
    private static final double MIN_VALID_DISTANCE_METERS = -150.0;

    // Threshold in meters where capture window ends
    private static final double CAPTURE_STOP_THRESHOLD_METERS = 20.0;

    // Maximum valid distance in meters (downstream boundary)
    private static final double MAX_VALID_DISTANCE_METERS = 90.0;

    // Internal state for the current tracked vehicle (in meters)
    private Double lastDistanceMeters = null;
    private boolean leavingEventSent = false;

    private final UnitConversionService unitConversionService;

    public RadarDataCollectorService(UnitConversionService unitConversionService) {
        this.unitConversionService = unitConversionService;
    }

    /*
     * Processes a single radar measurement and decides whether to
     * forward a RadarSample into the pipeline or discard it.
     */
    public Optional<RadarSample> processRadarData(RadarData radarData) {
        if (radarData == null) {
            return Optional.empty();
        }

        // External input is in miles; convert to meters for zone checks
        double distanceMiles = radarData.getDistanceMiles();
        double distanceMeters = unitConversionService.milesToMeters(distanceMiles);
        double speedMph = radarData.getSpeedMph();

        // Too far upstream → discard and reset state
        if (distanceMeters <= MIN_VALID_DISTANCE_METERS) {
            resetState();
            return Optional.empty();
        }

        // Too far downstream → discard and reset state
        if (distanceMeters > MAX_VALID_DISTANCE_METERS) {
            resetState();
            return Optional.empty();
        }

        // Active monitoring zone: always forward samples
        if (distanceMeters <= CAPTURE_STOP_THRESHOLD_METERS) {
            RadarSample sample = buildSample(distanceMiles, speedMph);
            lastDistanceMeters = distanceMeters;
            leavingEventSent = false;
            return Optional.of(sample);
        }

        // Leaving zone: forward only the first sample crossing > 20m
        boolean justCrossedBoundary =
                lastDistanceMeters != null
                        && lastDistanceMeters <= CAPTURE_STOP_THRESHOLD_METERS
                        && distanceMeters > CAPTURE_STOP_THRESHOLD_METERS
                        && !leavingEventSent;

        if (justCrossedBoundary) {
            RadarSample sample = buildSample(distanceMiles, speedMph);
            lastDistanceMeters = distanceMeters;
            leavingEventSent = true;
            return Optional.of(sample);
        }

        // Already in leaving zone after stop-capture event,
        // or started tracking when the vehicle was already > 20m
        lastDistanceMeters = distanceMeters;
        return Optional.empty();
    }

    // Builds a RadarSample using the original distance in miles
    private RadarSample buildSample(double distanceMiles, double speedMph) {
        return RadarSample.builder()
                .distanceMiles(distanceMiles)
                .speedMph(speedMph)
                .timestampMillis(System.currentTimeMillis())
                .targetId(1L) // simplified single target
                .build();
    }

    // Resets internal tracking state for the next vehicle
    private void resetState() {
        lastDistanceMeters = null;
        leavingEventSent = false;
    }
}
