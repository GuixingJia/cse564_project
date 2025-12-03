package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.CameraData;
import edu.asu.cse564.cse564_project.domain.PlateInfo;
import edu.asu.cse564.cse564_project.domain.SpeedContext;
import edu.asu.cse564.cse564_project.domain.ViolationRecord;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Evidence Collector and Packager
 *
 *   输入：
 *     - SpeedContext（来自 EvidenceCaptureController，表示本次超速/位置上下文）
 *     - PlateInfo（来自 ANPR Processor，车牌识别结果）
 *     - CameraData（来自 Camera Data Collector / 当前抓拍帧）
 *
 *   输出：
 *     - ViolationRecord（组合速度 + 车牌 + 图像 + 时间等信息）
 *
 * 设计假设：
 *   - 只有在车辆发生超速（overspeed == true）且位于监控/抓拍范围内时，
 *     上游才会调用本服务来打包记录；
 *   - 如果任何关键输入为空，当前实现会返回 Optional.empty()
 *     （即本次不生成违章记录）。
 */
@Service
public class EvidenceCollectorAndPackagerService {

    /**
     * Build a ViolationRecord from the given inputs.
     *
     * @param speedContext context produced by SpeedViolationController/ECC
     * @param plateInfo    ANPR recognition result
     * @param cameraData   captured image frame
     * @return Optional<ViolationRecord>:
     *         - present: valid violation record constructed
     *         - empty:   missing inputs or not overspeed
     */
    public Optional<ViolationRecord> buildViolationRecord(
            SpeedContext speedContext,
            PlateInfo plateInfo,
            CameraData cameraData
    ) {
        // 基本空值检查：任意关键参数为空则直接不生成记录
        if (speedContext == null || plateInfo == null || cameraData == null) {
            return Optional.empty();
        }

        // 安全防御逻辑：尽管上游理论上只会在 overspeed 时调用，
        // 这里仍额外检查一次，以避免误用。
        if (!speedContext.isOverspeed()) {
            return Optional.empty();
        }

        // 生成本地唯一的违章记录ID
        String violationId = UUID.randomUUID().toString();

        ViolationRecord record = ViolationRecord.builder()
                .violationId(violationId)
                .plateNumber(plateInfo.getPlateNumber())
                .speedMph(speedContext.getSpeedMph())
                .distanceMiles(speedContext.getDistanceMiles())
                .distanceMeters(speedContext.getDistanceMeters())
                .timestampMillis(speedContext.getTimestampMillis())
                .targetId(speedContext.getTargetId())
                .imageBytes(cameraData.getImageBytes())
                .build();

        return Optional.of(record);
    }
}
