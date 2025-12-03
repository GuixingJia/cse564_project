package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Output to the LEDDisplayController.
 *
 * 对应事件：
 *   event(SpeedStatus) speed_status
 *   speed_status > svc_out_led, led_ctrl_in
 *
 * 包含：
 *  - 当前速度（mph）
 *  - 当前距离（mile）
 *  - 是否超速（布尔）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeedStatus {

    /**
     * Current vehicle speed in miles per hour (mph).
     */
    private double speedMph;

    /**
     * Longitudinal distance from the device, in miles.
     */
    private double distanceMiles;

    /**
     * Whether the current speed is considered overspeed
     * (>= speed limit + tolerance).
     */
    private boolean overspeed;
}
