package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.RadarData;
import edu.asu.cse564.cse564_project.domain.RadarSample;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/*
 * Radar Data Collector
 *
 * 职责：
 *  - 接收来自 Radar/LiDAR 的 RadarData（单位：mile / mph）
 *  - 生成带时间戳和 targetId 的 RadarSample
 *  - 维护上一帧的距离，用于判断是否还在有效区、车辆是接近还是远离
 *
 * 逻辑（基于距离）：
 *  1. 保存上一轮输入的距离 lastDistanceMiles。
 *  2. 当前距离转换为米后：
 *     - 如果当前距离在 (-20m, 20m) 且大于上一轮距离（说明车辆在远离设备）→ 不再输出（不触发后续）
 *     - 如果当前距离 > 20m，说明车辆脱离有效监测区 → 不再输出
 *     - 其他情况：生成 RadarSample，交给后续的 SpeedViolationController 处理
 */
@Service
public class RadarDataCollectorService {

    // 有效区的距离阈值（米）
    private static final double EFFECTIVE_ZONE_METERS = 20.0;

    private final UnitConversionService unitConversionService;

    // 简单的 targetId 生成器
    private final AtomicLong targetIdSequence = new AtomicLong(0L);

    // 记录上一帧的距离（单位：mile），初始为 null
    private Double lastDistanceMiles = null;

    public RadarDataCollectorService(UnitConversionService unitConversionService) {
        this.unitConversionService = unitConversionService;
    }

    /*
     * 处理一条来自雷达的原始数据，返回一个可选的 RadarSample。
     * 如果根据距离判断不需要后续处理（比如车辆已经离开有效区），则返回 Optional.empty()。
     *
     * @param radarData raw radar input (distance in miles, speed in mph)
     * @return Optional<RadarSample> - present only when the sample should
     *         be forwarded to SpeedViolationController.
     */
    public Optional<RadarSample> processRadarData(RadarData radarData) {
        if (radarData == null) {
            return Optional.empty();
        }

        double currentDistanceMiles = radarData.getDistanceMiles();
        double currentSpeedMph = radarData.getSpeedMph();

        // 将当前距离转换为米，用于与 (-20, 20) m 的几何区域比较
        double currentDistanceMeters =
                unitConversionService.milesToMeters(currentDistanceMiles);

        Double previousDistanceMilesLocal = this.lastDistanceMiles;
        double previousDistanceMeters =
                previousDistanceMilesLocal == null
                        ? Double.NaN
                        : unitConversionService.milesToMeters(previousDistanceMilesLocal);

        // 更新 lastDistanceMiles（无论是否输出，后续判断都需要）
        this.lastDistanceMiles = currentDistanceMiles;

        // 1) 如果当前距离 > 20m：车辆已经离开有效监测区，不再输出
        if (currentDistanceMeters > EFFECTIVE_ZONE_METERS) {
            return Optional.empty();
        }

        // 2) 如果当前距离在 (-20m, 20m) 且上一帧存在，
        //    并且当前距离 > 上一帧距离（车辆正在远离设备），也不再输出
        if (!Double.isNaN(previousDistanceMeters)
                && currentDistanceMeters > -EFFECTIVE_ZONE_METERS
                && currentDistanceMeters < EFFECTIVE_ZONE_METERS
                && currentDistanceMeters > previousDistanceMeters) {
            return Optional.empty();
        }

        // 3) 其他情况：生成一个 RadarSample 交给下游
        long now = System.currentTimeMillis();
        long targetId = targetIdSequence.incrementAndGet();

        RadarSample sample = RadarSample.builder()
                .distanceMiles(currentDistanceMiles)
                .speedMph(currentSpeedMph)
                .timestampMillis(now)
                .targetId(targetId)
                .build();

        return Optional.of(sample);
    }

    /*
     * 重置 collector 状态。
     * 当检测到一辆车处理完毕后，可以调用此方法清理状态。
     */
    public void resetState() {
        this.lastDistanceMiles = null;
    }
}
