package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result structure returned by EvidenceCaptureControllerService.
 *
 * This structure maps directly to the SRC "capture_cmd" event
 * which drives both Camera and Flash simultaneously.
 *
 * captureActive:
 *   - TRUE  -> Start/continue capturing (Camera+Flash ON)
 *   - FALSE -> Stop capturing / enter sleep (Camera+Flash OFF)
 *   - null  -> No command issued for this sample
 *
 * speedContext:
 *   - Non-null -> Send to EvidenceCollectorAndPackager
 *   - Null     -> Do not forward
 *
 * This version fully supports Design Plan C:
 *   - SpeedViolationController ALWAYS sends an "end-of-tracking" context
 *     when the vehicle exits the monitored region (>= +20 meters).
 *   - EvidenceCaptureController uses distanceMeters to issue an explicit
 *     stop command (captureActive = FALSE), ensuring Flash & Camera are
 *     always turned off reliably.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceCaptureResult {

    /**
     * Unified boolean command for Camera + Flash:
     *
     * TRUE  -> Capture active (turn ON)
     * FALSE -> Capture inactive (turn OFF)
     * null  -> No output command
     */
    private Boolean captureActive;

    /**
     * Optional SpeedContext forwarded to the evidence packager.
     * Null means "do not forward".
     */
    private SpeedContext speedContext;
}
