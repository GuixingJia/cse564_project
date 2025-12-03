package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command object sent from the LEDDisplayController to the physical LED display.
 *   event(LedCommand) led_cmd
 *   led_cmd > led_ctrl_out, led_disp_in
 * 这里把要展示的信息格式化成字符串，同时保留一些原始字段：
 *  - speedMph       当前速度
 *  - distanceMiles  当前距离
 *  - overspeed      是否超速
 *  - message        最终要显示在 LED 上的文本
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedCommand {


     // Current vehicle speed in mph.
    private double speedMph;

    // Distance from the radar in miles.
    private double distanceMiles;

    // Whether the vehicle is overspeed.
    private boolean overspeed;

    /*
     * Text message to be shown on the LED display.
     * Example:
     *   "Speed: 38 mph - OK"
     *   "OVERSPEED: 52 mph - SLOW DOWN"
     */
    private String message;
}
