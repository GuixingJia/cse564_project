package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.CameraData;
import edu.asu.cse564.cse564_project.domain.PlateInfo;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/*
 * ANPR Processor
 *
 * 对应设计中的组件：
 *   - 输入：CameraData（来自 Camera Data Collector）
 *   - 输出：PlateInfo（车牌识别结果）
 *
 * 当前为模拟实现：
 *   - 内置三个随机车牌号
 *   - 只要 CameraData 非 null，就从列表里随机选择一个 PlateInfo 返回
 */
@Service
public class AnprProcessorService {

    private final Random random = new Random();

    /*
     * 模拟车牌号数据库：你可以随时替换为更真实的。
     */
    private static final List<String> MOCK_PLATE_NUMBERS = Arrays.asList(
            "ABC-1234",
            "NXY-4821",
            "JDK-9087"
    );

    /**
     * Process a camera frame and return a randomly chosen plate number.
     *
     * @param cameraData preprocessed frame from CameraDataCollector
     * @return Optional<PlateInfo>:
     *         - present: always true if cameraData != null
     *         - empty: if input is null
     */
    public Optional<PlateInfo> processFrame(CameraData cameraData) {
        if (cameraData == null) {
            return Optional.empty();
        }

        // 在列表中随机挑一个车牌号
        String plate = MOCK_PLATE_NUMBERS.get(
                random.nextInt(MOCK_PLATE_NUMBERS.size())
        );

        PlateInfo info = PlateInfo.builder()
                .plateNumber(plate)
                .timestampMillis(System.currentTimeMillis())
                .build();

        return Optional.of(info);
    }
}
