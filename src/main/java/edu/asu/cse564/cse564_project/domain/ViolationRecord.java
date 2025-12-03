package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Final packaged violation record that will be uploaded
 * to the Central Backend System.
 *
 *   event(ViolationRecord) violation_record
 *   violation_record > packer_out, uplink_in
 *
 * 包含核心证据信息：
 *  - 车牌号
 *  - 车辆速度（mph）
 *  - 车辆距离（mile / meter）
 *  - 触发时间（timestamp）
 *  - 目标ID（用于区分车辆/轨迹）
 *  - 原始图像数据（可以是缩略图/关键帧）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationRecord {

    /*
     * Unique ID assigned by the roadside unit for this violation.
     * 简单实现：可以由服务端自增生成。
     */
    private String violationId;

    /*
     * License plate number recognized by ANPR.
     */
    private String plateNumber;

    /*
     * Vehicle speed in miles per hour (mph) at the time of violation.
     */
    private double speedMph;

    /*
     * Distance from the device in miles.
     */
    private double distanceMiles;

    /*
     * Distance from the device in meters (for analysis / reconstruction).
     */
    private double distanceMeters;

    /*
     * Timestamp of the violation event in milliseconds since Unix epoch.
     * 这里可以使用 SpeedContext 的 timestampMillis。
     */
    private long timestampMillis;

    /*
     * Target identifier from the radar tracking (if available).
     */
    private long targetId;

    /*
     * Captured image bytes associated with this violation.
     * 可以是从对应 CameraData 中取出的那一帧图片。
     */
    private byte[] imageBytes;
}