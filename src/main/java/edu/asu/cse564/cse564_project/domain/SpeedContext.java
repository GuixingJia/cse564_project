package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Context for an overspeed event, to be consumed by
 * the EvidenceCaptureController and the EvidenceCollector.
 *
 * 对应事件：
 *   event(SpeedContext) overspeed_ctx
 *   overspeed_ctx > svc_out_viol, ecc_in
 *
 * 包含：
 *  - 是否超速（一般为 true）
 *  - 速度（mph）
 *  - 距离（mile & meters）
 *  - 时间戳（ms）
 *  - targetId（来自 RadarSample，用来区分车辆）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeedContext {

    /**
     * Whether the current sample is overspeed.
     * 在正常流程中，如果不是 overspeed，就不会产生 SpeedContext。
     */
    private boolean overspeed;

    /**
     * Vehicle speed in miles per hour (mph).
     */
    private double speedMph;

    /**
     * Distance in miles (external representation).
     */
    private double distanceMiles;

    /**
     * Distance in meters (for internal geometry / zone checks).
     */
    private double distanceMeters;

    /**
     * Timestamp in milliseconds since Unix epoch.
     */
    private long timestampMillis;

    /**
     * Target identifier (from RadarSample).
     */
    private long targetId;
}
