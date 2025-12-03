package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.RadarSample;
import edu.asu.cse564.cse564_project.domain.SpeedContext;
import edu.asu.cse564.cse564_project.domain.SpeedStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

/*
 * Speed Violation Controller
 *
 * 对应组件：
 *  - 输入：RadarSample（来自 RadarDataCollector）
 *  - 输出：
 *      event(SpeedStatus) speed_status
 *      event(SpeedContext) overspeed_ctx
 *
 * 逻辑：
 *  - 限速：40 mph
 *  - 容差：10%（即阈值 = 40 * 1.1 = 44 mph）
 *  - 判断是否超速
 *  - 始终输出 SpeedStatus（给 LED）
 *  - 仅在超速时输出 SpeedContext（给 Evidence Capture Controller）
 */
@Service
public class SpeedViolationControllerService {

    // Base speed limit in mph (e.g., construction zone limit).
    private static final double SPEED_LIMIT_MPH = 40.0;

    // Tolerance ratio (10% over the speed limit).
    private static final double TOLERANCE_RATIO = 0.10;

    private final UnitConversionService unitConversionService;

    public SpeedViolationControllerService(UnitConversionService unitConversionService) {
        this.unitConversionService = unitConversionService;
    }

    /**
     * 构造 SpeedStatus，给 LED Display 使用。
     *
     * @param sample enriched radar sample
     * @return SpeedStatus
     */
    public SpeedStatus buildSpeedStatus(RadarSample sample) {
        boolean overspeed = isOverspeed(sample.getSpeedMph());
        return SpeedStatus.builder()
                .speedMph(sample.getSpeedMph())
                .distanceMiles(sample.getDistanceMiles())
                .overspeed(overspeed)
                .build();
    }

    /**
     * 构造 SpeedContext，仅在 overspeed 时返回。
     * 如果没有超速，则返回 Optional.empty()，表示不会发出 overspeed_ctx 事件。
     *
     * @param sample enriched radar sample
     * @return Optional<SpeedContext>
     */
    public Optional<SpeedContext> buildOverspeedContext(RadarSample sample) {
        boolean overspeed = isOverspeed(sample.getSpeedMph());
        if (!overspeed) {
            return Optional.empty();
        }

        double distanceMiles = sample.getDistanceMiles();
        double distanceMeters = unitConversionService.milesToMeters(distanceMiles);

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

    /**
     * 内部方法：根据当前速度判断是否超速。
     * overspeed 条件：
     *   speed >= SPEED_LIMIT_MPH * (1 + TOLERANCE_RATIO)
     *
     * @param speedMph current speed in mph
     * @return true if overspeed
     */
    private boolean isOverspeed(double speedMph) {
        double threshold = SPEED_LIMIT_MPH * (1.0 + TOLERANCE_RATIO);
        return speedMph >= threshold;
    }
}
