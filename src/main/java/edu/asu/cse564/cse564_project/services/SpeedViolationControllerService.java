package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.RadarSample;
import edu.asu.cse564.cse564_project.domain.SpeedContext;
import edu.asu.cse564.cse564_project.domain.SpeedStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

/*
 * Speed Violation Controller
 *
 * - 输入：RadarSample（来自 RadarDataCollector）
 * - 输出：
 *      event(SpeedStatus) speed_status  → ALWAYS (for LED)
 *      event(SpeedContext) overspeed_ctx → ONLY when overspeed AND within monitoring zone
 *
 * 距离逻辑（基于 distanceMeters）：
 *
 *   d <= -150m                  : 已被 Radar 丢弃（不会进入本组件）
 *   -150m < d <= -90m (coarse)  : 只做超速判定，不向 ECC 输出上下文
 *   -90m < d        (monitor)   : 若超速 → 输出 SpeedContext 给 ECC
 *
 * 抓拍窗口 (-20m, 20m) 和 d >= 20m 的停止抓拍，由 ECC 再细分处理。
 */
@Service
public class SpeedViolationControllerService {

    // Base speed limit in mph (e.g., construction zone limit).
    private static final double SPEED_LIMIT_MPH = 40.0;

    // Tolerance ratio (10% over the speed limit).
    private static final double TOLERANCE_RATIO = 0.10;

    // Boundary between "coarse-only" and "active monitoring" (in meters).
    // Radar 已保证 d > -150m，这里再把 -90m 作为 SVC/ECC 开始工作的下界。
    private static final double MONITOR_ZONE_START_METERS = -90.0;

    private final UnitConversionService unitConversionService;

    public SpeedViolationControllerService(UnitConversionService unitConversionService) {
        this.unitConversionService = unitConversionService;
    }

    /*
     * Build SpeedStatus for LED Display.
     * Always generated regardless of distance (LED 控制器可以再根据距离决定是否显示).
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
     * Build SpeedContext only when:
     *   - overspeed, AND
     *   - distanceMeters > MONITOR_ZONE_START_METERS (i.e., beyond -90m)
     *
     * 在粗测区 (-150m, -90m] 内，即使超速也不会产生 SpeedContext，
     * 数据只停留在 SpeedStatus / 后端统计，不触发 ECC。
     */
    public Optional<SpeedContext> buildOverspeedContext(RadarSample sample) {
        boolean overspeed = isOverspeed(sample.getSpeedMph());
        if (!overspeed) {
            return Optional.empty();
        }

        double distanceMiles = sample.getDistanceMiles();
        double distanceMeters = unitConversionService.milesToMeters(distanceMiles);

        // In the "coarse-only" region, do not send context to ECC.
        if (distanceMeters <= MONITOR_ZONE_START_METERS) {
            return Optional.empty();
        }

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

    /*
     * overspeed 条件：
     *   speed >= SPEED_LIMIT_MPH * (1 + TOLERANCE_RATIO)
     */
    private boolean isOverspeed(double speedMph) {
        double threshold = SPEED_LIMIT_MPH * (1.0 + TOLERANCE_RATIO);
        return speedMph >= threshold;
    }
}
