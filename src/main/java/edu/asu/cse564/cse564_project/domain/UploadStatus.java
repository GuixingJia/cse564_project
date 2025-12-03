package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Result of attempting to upload a ViolationRecord to the
 * Central Backend System.
 *
 * 对应设计中的 Backend Up Link Controller 对外可见的输出状态：
 *   - 是否上传成功
 *   - 后端返回的记录ID（如果有）
 *   - 重试次数
 *   - 描述信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadStatus {

    /*
     * Whether the upload is considered successful.
     */
    private boolean success;

    /*
     * Optional backend-assigned identifier for the violation
     * after it is stored in the central system.
     */
    private String backendRecordId;

    /*
     * Number of retry attempts that were performed before success/failure.
     */
    private int retryCount;

    /*
     * Human-readable message for logging/debug.
     */
    private String message;

    /*
     * Timestamp when this upload attempt completed, in ms since epoch.
     */
    private long timestampMillis;
}
