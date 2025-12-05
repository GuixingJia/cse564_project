package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.EvidenceCaptureResult;
import edu.asu.cse564.cse564_project.domain.SpeedContext;
import org.springframework.stereotype.Service;

/*
 * EvidenceCaptureControllerService
 *
 * Determines whether the camera and flash should be active based on vehicle
 * distance and whether the SpeedContext should be forwarded for evidence
 * packaging. The controller does not evaluate overspeed; it only reacts
 * to position (in meters).
 *
 * Distance-based behavior:
 *   1) distance <= -20m      : Not yet in capture zone → no command, no context
 *   2) -20m < distance < 20m : Inside capture window → activate capture + forward context
 *   3) distance >= 20m       : Leaving capture zone → stop capture, do not forward context
 *
 * Assumes SpeedViolationController will send at least one sample where
 * distance >= +20m to trigger a stop-capture signal.
 */
@Service
public class EvidenceCaptureControllerService {

    // Capture window half-range in meters
    private static final double CAPTURE_WINDOW_METERS = 20.0;

    /*
     * Evaluates SpeedContext and determines capture activation and context forwarding.
     * Returns an EvidenceCaptureResult describing the decision.
     */
    public EvidenceCaptureResult handleSpeedContext(SpeedContext context) {
        if (context == null) {
            return EvidenceCaptureResult.builder().build();
        }

        double distanceMeters = context.getDistanceMeters();

        // Case 1: Vehicle is before the capture window
        if (distanceMeters <= -CAPTURE_WINDOW_METERS) {
            return EvidenceCaptureResult.builder()
                    .captureActive(null)
                    .speedContext(null)
                    .build();
        }

        // Case 2: Vehicle is inside the capture window
        if (distanceMeters > -CAPTURE_WINDOW_METERS && distanceMeters < CAPTURE_WINDOW_METERS) {
            return EvidenceCaptureResult.builder()
                    .captureActive(Boolean.TRUE)
                    .speedContext(context)
                    .build();
        }

        // Case 3: Vehicle has passed beyond the capture zone
        return EvidenceCaptureResult.builder()
                .captureActive(Boolean.FALSE)
                .speedContext(null)
                .build();
    }
}
