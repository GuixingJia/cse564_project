package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.RadarData;
import edu.asu.cse564.cse564_project.domain.RadarSample;
import org.springframework.stereotype.Service;

import java.util.Optional;

/*
 * Radar Data Collector
 *
 * Distance zones (meters):
 *
 *   d <= -150m          : OUT_OF_RANGE_BEFORE  → discard
 *   -150m < d <= 20m    : ACTIVE_MONITOR_ZONE  → always forward samples
 *   20m < d <= 90m      : LEAVING_ZONE         → forward ONLY the first sample that crosses >20m
 *   d > 90m             : OUT_OF_RANGE_AFTER   → discard and reset state
 *
 * This implementation assumes a single tracked vehicle whose distance
 * increases monotonically as it passes the device.
 */
@Service
public class RadarDataCollectorService {

    private static final double MIN_VALID_DISTANCE_METERS = -150.0;
    private static final double CAPTURE_STOP_THRESHOLD_METERS = 20.0;
    private static final double MAX_VALID_DISTANCE_METERS = 90.0;

    // Internal state for the current tracked vehicle (in meters)
    private Double lastDistanceMeters = null;
    private boolean leavingEventSent = false;

    private final UnitConversionService unitConversionService;

    public RadarDataCollectorService(UnitConversionService unitConversionService) {
        this.unitConversionService = unitConversionService;
    }

    public Optional<RadarSample> processRadarData(RadarData radarData) {
        if (radarData == null) {
            return Optional.empty();
        }

        // 外部输入是英里，这里显式拆成 miles 和 meters 两种表示
        double distanceMiles  = radarData.getDistanceMiles();
        double distanceMeters = unitConversionService.milesToMeters(distanceMiles);
        double speedMph       = radarData.getSpeedMph();

        // 0. Too far upstream: d <= -150m → discard and reset state
        if (distanceMeters <= MIN_VALID_DISTANCE_METERS) {
            resetState();
            return Optional.empty();
        }

        // 4. Too far downstream: d > 90m → discard and reset state
        if (distanceMeters > MAX_VALID_DISTANCE_METERS) {
            resetState();
            return Optional.empty();
        }

        // 1. Active monitoring zone: -150m < d <= 20m
        if (distanceMeters <= CAPTURE_STOP_THRESHOLD_METERS) {
            // In this zone we always forward samples
            RadarSample sample = buildSample(distanceMiles, speedMph);
            // Update internal state (用 meters 记录轨迹位置)
            lastDistanceMeters = distanceMeters;
            leavingEventSent = false; // still inside or before capture/stop threshold
            return Optional.of(sample);
        }

        // 2. Leaving zone: 20m < d <= 90m
        // We only want to forward the FIRST sample that crosses from <=20m to >20m.
        boolean justCrossedBoundary =
                lastDistanceMeters != null
                        && lastDistanceMeters <= CAPTURE_STOP_THRESHOLD_METERS
                        && distanceMeters > CAPTURE_STOP_THRESHOLD_METERS
                        && !leavingEventSent;

        if (justCrossedBoundary) {
            // Forward ONE "leaving" sample so that ECC can issue stop-capture.
            RadarSample sample = buildSample(distanceMiles, speedMph);
            lastDistanceMeters = distanceMeters;
            leavingEventSent = true;
            return Optional.of(sample);
        }

        // Already in leaving zone and we have sent the stop-capture event,
        // or we started tracking when the vehicle was already >20m:
        // do not forward any more samples.
        lastDistanceMeters = distanceMeters;
        return Optional.empty();
    }

    private RadarSample buildSample(double distanceMiles, double speedMph) {
        return RadarSample.builder()
                .distanceMiles(distanceMiles)          // 这里是真正的“英里”值
                .speedMph(speedMph)
                .timestampMillis(System.currentTimeMillis())
                .targetId(1L) // simplified single target
                .build();
    }

    private void resetState() {
        lastDistanceMeters = null;
        leavingEventSent = false;
    }
}
