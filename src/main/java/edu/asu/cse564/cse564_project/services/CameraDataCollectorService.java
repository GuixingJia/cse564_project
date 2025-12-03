package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.CameraData;
import org.springframework.stereotype.Service;

import java.util.Optional;

/*
 * Camera Data Collector
 *
 * 对应设计中的组件：
 *   - 输入：来自摄像头的 CameraData（raw image frame）
 *   - 输出：预处理后的 CameraData，交给 ANPR Processor
 *
 * 当前实现为模拟：
 *   - 只要传入的 CameraData 非 null 且 imageBytes 非空，就认为是有效帧
 *   - 不做任何图像处理（可在未来扩展）
 */
@Service
public class CameraDataCollectorService {

    /**
     * Process the raw camera frame and decide whether it should be forwarded
     * to the ANPR processor.
     *
     * @param rawFrame raw frame from the simulated camera input
     * @return Optional<CameraData>:
     *         - present: frame passed basic validation
     *         - empty: frame rejected
     */
    public Optional<CameraData> processCameraFrame(CameraData rawFrame) {
        if (rawFrame == null) {
            return Optional.empty();
        }

        byte[] imageBytes = rawFrame.getImageBytes();

        // Basic validation: ensure the image byte array is valid
        if (imageBytes == null || imageBytes.length == 0) {
            return Optional.empty();
        }

        // In a real system, image preprocessing could occur here.
        // For now, we simply return the raw frame unchanged.
        return Optional.of(rawFrame);
    }
}
