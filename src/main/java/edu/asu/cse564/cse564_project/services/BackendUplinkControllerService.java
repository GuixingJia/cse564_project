package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.UploadStatus;
import edu.asu.cse564.cse564_project.domain.ViolationRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/*
 * Backend Up Link Controller
 *
 *   输入：ViolationRecord（来自 EvidenceCollectorAndPackager）
 *   输出：上传到 Central Backend System，并返回 UploadStatus
 *
 * 当前实现为模拟：
 *   - 用一个本地内存列表 buffer 当作“待上传队列/本地缓存”
 *   - uploadViolationRecord(...) 会：
 *       1. 简单检查 ViolationRecord 是否有效
 *       2. 将其加入本地缓存
 *       3. 模拟“上传成功”，生成一个 backendRecordId
 *
 * 如果要接真实后端，在这里：
 *   - 使用 HTTP 客户端（RestTemplate/WebClient）调用远程服务
 *   - 在失败时重试并更新 retryCount
 */
@Service
public class BackendUplinkControllerService {

    /*
     * Simple in-memory buffer simulating a local queue/cache
     * for violation records that have been "uploaded" or are
     * waiting to be confirmed.
     */
    private final List<ViolationRecord> localBuffer =
            Collections.synchronizedList(new ArrayList<>());

    /*
     * Simulate uploading a violation record to the central backend.
     *
     * @param record violation record produced by the packager
     * @return UploadStatus describing the outcome
     */
    public UploadStatus uploadViolationRecord(ViolationRecord record) {
        long now = System.currentTimeMillis();

        if (record == null) {
            return UploadStatus.builder()
                    .success(false)
                    .retryCount(0)
                    .backendRecordId(null)
                    .message("ViolationRecord is null, nothing to upload.")
                    .timestampMillis(now)
                    .build();
        }

        //   - 实际 HTTP 调用远程后端
        //   - 捕获异常并进行重试
        //   - 根据后端返回的 ID 填充 backendRecordId
        // 当前简化为：认为上传总是成功，并生成一个模拟 backendRecordId。

        String backendRecordId = UUID.randomUUID().toString();

        // 将记录加入本地缓冲（模拟已上传的本地备份）
        localBuffer.add(record);

        return UploadStatus.builder()
                .success(true)
                .retryCount(0)
                .backendRecordId(backendRecordId)
                .message("ViolationRecord uploaded (simulated) successfully.")
                .timestampMillis(now)
                .build();
    }

    /*
     * For debugging / monitoring:
     * 返回当前本地缓存中的所有违章记录的快照。
     */
    public List<ViolationRecord> getBufferedRecordsSnapshot() {
        synchronized (localBuffer) {
            return new ArrayList<>(localBuffer);
        }
    }

    /*
     * 清空本地缓存，用于测试或重置系统状态。
     */
    public void clearBuffer() {
        localBuffer.clear();
    }
}
