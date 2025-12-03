package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.LedCommand;
import edu.asu.cse564.cse564_project.domain.SpeedStatus;
import org.springframework.stereotype.Service;

/*
 * LED Display Controller
 *
 *   输入：SpeedStatus（来自 SpeedViolationController）
 *   输出：LedCommand（控制物理 LED 显示屏）
 *
 * 逻辑：
 *   - 始终显示当前速度（mph）
 *   - 如果未超速：显示“OK” 或类似提示
 *   - 如果超速：高亮提示（例如 "OVERSPEED: xx mph - SLOW DOWN"）
 */
@Service
public class LedDisplayControllerService {

    /**
     * Build a LedCommand from the given SpeedStatus.
     *
     * @param status SpeedStatus from SpeedViolationController
     * @return LedCommand to be sent to the physical LED display
     */
    public LedCommand buildLedCommand(SpeedStatus status) {
        if (status == null) {
            // 极端防御情况：返回一个默认的“无数据”消息
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
            // 超速时，构建警告信息
            message = String.format(
                    "OVERSPEED: %.1f mph - SLOW DOWN",
                    speedMph
            );
        } else {
            // 未超速时，正常显示当前速度
            message = String.format(
                    "Speed: %.1f mph - OK",
                    speedMph
            );
        }

        return LedCommand.builder()
                .speedMph(speedMph)
                .distanceMiles(distanceMiles)
                .overspeed(overspeed)
                .message(message)
                .build();
    }
}
