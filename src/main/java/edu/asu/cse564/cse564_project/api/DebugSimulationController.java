package edu.asu.cse564.cse564_project.api;

import edu.asu.cse564.cse564_project.domain.*;
import edu.asu.cse564.cse564_project.services.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * DebugSimulationController
 *
 * 提供调试用接口，串起整条流水线：
 *
 * RadarData -> RadarDataCollector -> RadarSample
 * -> SpeedViolationController (SpeedStatus + SpeedContext)
 * -> LEDDisplayController (LedCommand)
 * -> EvidenceCaptureController (captureActive + SpeedContext)
 * -> CameraDataCollector -> CameraData
 * -> ANPR (PlateInfo)
 * -> EvidenceCollectorAndPackager (ViolationRecord)
 * -> BackendUpLinkController (UploadStatus)
 *
 * 1) /api/debug/simulate
 *    模拟一次“车辆超速并被抓拍上传”的完整流程。
 *
 * 2) /api/debug/simulateNormal?speedMph=30
 *    模拟一次“车辆正常行驶（未超速）”的流程，只走到 LED，后续链路不触发。
 */
@RestController
public class DebugSimulationController {

    private final RadarDataCollectorService radarDataCollectorService;
    private final SpeedViolationControllerService speedViolationControllerService;
    private final LedDisplayControllerService ledDisplayControllerService;
    private final EvidenceCaptureControllerService evidenceCaptureControllerService;
    private final CameraDataCollectorService cameraDataCollectorService;
    private final AnprProcessorService anprProcessorService;
    private final EvidenceCollectorAndPackagerService evidenceCollectorAndPackagerService;
    private final BackendUplinkControllerService backendUplinkControllerService;

    public DebugSimulationController(
            RadarDataCollectorService radarDataCollectorService,
            SpeedViolationControllerService speedViolationControllerService,
            LedDisplayControllerService ledDisplayControllerService,
            EvidenceCaptureControllerService evidenceCaptureControllerService,
            CameraDataCollectorService cameraDataCollectorService,
            AnprProcessorService anprProcessorService,
            EvidenceCollectorAndPackagerService evidenceCollectorAndPackagerService,
            BackendUplinkControllerService backendUplinkControllerService
    ) {
        this.radarDataCollectorService = radarDataCollectorService;
        this.speedViolationControllerService = speedViolationControllerService;
        this.ledDisplayControllerService = ledDisplayControllerService;
        this.evidenceCaptureControllerService = evidenceCaptureControllerService;
        this.cameraDataCollectorService = cameraDataCollectorService;
        this.anprProcessorService = anprProcessorService;
        this.evidenceCollectorAndPackagerService = evidenceCollectorAndPackagerService;
        this.backendUplinkControllerService = backendUplinkControllerService;
    }

    /**
     * 1) 模拟一次“车辆超速并被抓拍上传”的完整流程。
     *
     * 访问：GET /api/debug/simulate
     */
    @GetMapping("/api/debug/simulate")
    public Map<String, Object> simulateOneViolation() {

        // 1. 构造一条模拟的雷达输入：距离在抓拍区附近、速度明显超速
        //    SpeedViolationController 中阈值 = 40 * 1.1 = 44 mph
        RadarData radarData = RadarData.builder()
                .distanceMiles(0.0)  // 接近设备
                .speedMph(50.0)      // 明显超速
                .build();

        // 2. Radar Data Collector
        Optional<RadarSample> maybeSample = radarDataCollectorService.processRadarData(radarData);
        if (maybeSample.isEmpty()) {
            return Map.of(
                    "success", false,
                    "stage", "RadarDataCollector",
                    "reason", "Sample filtered out by RadarDataCollector."
            );
        }
        RadarSample sample = maybeSample.get();

        // 3. Speed Violation Controller
        SpeedStatus speedStatus = speedViolationControllerService.buildSpeedStatus(sample);
        Optional<SpeedContext> maybeCtx = speedViolationControllerService.buildOverspeedContext(sample);
        if (maybeCtx.isEmpty()) {
            return Map.of(
                    "success", false,
                    "stage", "SpeedViolationController",
                    "reason", "No overspeed context produced (not overspeed?).",
                    "speedStatus", speedStatus
            );
        }
        SpeedContext speedContext = maybeCtx.get();

        // 4. LED Display Controller
        LedCommand ledCommand = ledDisplayControllerService.buildLedCommand(speedStatus);

        // 5. Evidence Capture Controller
        EvidenceCaptureResult eccResult = evidenceCaptureControllerService.handleSpeedContext(speedContext);
        Boolean captureActive = eccResult.getCaptureActive();
        if (!Boolean.TRUE.equals(captureActive)) {
            return Map.of(
                    "success", false,
                    "stage", "EvidenceCaptureController",
                    "reason", "ECC did not activate capture.",
                    "captureActive", captureActive,
                    "ledMessage", ledCommand.getMessage()
            );
        }

        SpeedContext ctxForPackager = eccResult.getSpeedContext();
        if (ctxForPackager == null) {
            return Map.of(
                    "success", false,
                    "stage", "EvidenceCaptureController",
                    "reason", "ECC did not forward SpeedContext to packager.",
                    "captureActive", captureActive
            );
        }

        // 6. 构造模拟的 CameraData
        byte[] fakeImage = "fakeImageBytes".getBytes(); // 仅测试用
        CameraData rawCameraData = CameraData.builder()
                .imageBytes(fakeImage)
                .timestampMillis(System.currentTimeMillis())
                .build();

        // Camera Data Collector
        Optional<CameraData> maybeProcessedFrame = cameraDataCollectorService.processCameraFrame(rawCameraData);
        if (maybeProcessedFrame.isEmpty()) {
            return Map.of(
                    "success", false,
                    "stage", "CameraDataCollector",
                    "reason", "CameraDataCollector rejected the frame."
            );
        }
        CameraData processedFrame = maybeProcessedFrame.get();

        // 7. ANPR Processor
        Optional<PlateInfo> maybePlate = anprProcessorService.processFrame(processedFrame);
        if (maybePlate.isEmpty()) {
            return Map.of(
                    "success", false,
                    "stage", "AnprProcessor",
                    "reason", "ANPR did not produce PlateInfo."
            );
        }
        PlateInfo plateInfo = maybePlate.get();

        // 8. Evidence Collector & Packager
        Optional<ViolationRecord> maybeRecord =
                evidenceCollectorAndPackagerService.buildViolationRecord(
                        ctxForPackager,
                        plateInfo,
                        processedFrame
                );

        if (maybeRecord.isEmpty()) {
            return Map.of(
                    "success", false,
                    "stage", "EvidenceCollectorAndPackager",
                    "reason", "No ViolationRecord produced."
            );
        }
        ViolationRecord record = maybeRecord.get();

        // 9. Backend Up Link Controller
        UploadStatus uploadStatus = backendUplinkControllerService.uploadViolationRecord(record);

        // 10. 汇总输出
        return Map.of(
                "success", true,
                "mode", "overspeed-violation",
                "ledMessage", ledCommand.getMessage(),
                "captureActive", captureActive,
                "plateNumber", plateInfo.getPlateNumber(),
                "violationRecord", record,
                "uploadStatus", uploadStatus
        );
    }

    /**
     * 2) 模拟一次“车辆正常行驶（未超速）”流程。
     *
     * 访问：GET /api/debug/simulateNormal?speedMph=30
     * speedMph 参数可选，默认 30 mph。
     */
    @GetMapping("/api/debug/simulateNormal")
    public Map<String, Object> simulateNormalDriving(
            @RequestParam(name = "speedMph", defaultValue = "30.0") double speedMph
    ) {

        // 1. 构造一条模拟的雷达输入：速度低于超速阈值
        RadarData radarData = RadarData.builder()
                .distanceMiles(0.0)   // 在工作区附近
                .speedMph(speedMph)   // 默认 30 mph，低于 44 mph 阈值
                .build();

        // 2. Radar Data Collector
        Optional<RadarSample> maybeSample = radarDataCollectorService.processRadarData(radarData);
        if (maybeSample.isEmpty()) {
            return Map.of(
                    "success", false,
                    "stage", "RadarDataCollector",
                    "reason", "Sample filtered out by RadarDataCollector."
            );
        }
        RadarSample sample = maybeSample.get();

        // 3. Speed Violation Controller
        SpeedStatus speedStatus = speedViolationControllerService.buildSpeedStatus(sample);
        Optional<SpeedContext> maybeCtx = speedViolationControllerService.buildOverspeedContext(sample);

        // 4. LED Display Controller
        LedCommand ledCommand = ledDisplayControllerService.buildLedCommand(speedStatus);

        // 5. 返回结果：正常行驶 -> 不应产生 overspeed context，不触发后续链路
        return Map.of(
                "success", true,
                "mode", "normal-driving",
                "inputSpeedMph", speedMph,
                "overspeedContextPresent", maybeCtx.isPresent(),  // 这里按设计应为 false
                "ledMessage", ledCommand.getMessage(),
                "speedStatus", speedStatus
        );
    }
}
