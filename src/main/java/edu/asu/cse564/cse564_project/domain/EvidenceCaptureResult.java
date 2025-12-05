package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * EvidenceCaptureResult
 *
 * Represents the output decision produced by the EvidenceCaptureController.
 * It combines two aspects:
 *   - captureActive: a unified command signal to control Camera + Flash
 *   - speedContext: optional contextual data forwarded for evidence packaging
 *
 * captureActive values:
 *   TRUE  — enable or continue capture
 *   FALSE — disable capture and enter sleep
 *   null  — no change to current hardware state
 *
 * speedContext:
 *   non-null — should be forwarded to the evidence collector
 *   null     — no evidence packaging for this sample
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceCaptureResult {

    // Command indicating whether camera + flash should be active
    private Boolean captureActive;

    // Context forwarded to evidence packaging (null means "not forwarded")
    private SpeedContext speedContext;
}
