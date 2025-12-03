package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of ANPR (Automatic Number Plate Recognition).
 *
 * 在当前实现中，这只是一个模拟结构：
 *  - system 会从一个静态车牌号列表中随机选一个返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlateInfo {

    /** Randomly chosen license plate number. */
    private String plateNumber;

    /** Timestamp of when this ANPR result was produced. */
    private long timestampMillis;
}
