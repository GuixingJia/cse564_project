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
 *    模拟一次“车辆超速并被抓拍上传”的完整流程（固定速度/距离）。
 *
 * 2) /api/debug/simulateNormal?speedMph=30
 *    模拟一次“车辆正常行驶（未超速）”的流程，只走到 LED。
 *
 * 3) /api/debug/simulateCase?speedMph=50&distanceMiles=0
 *    按输入的速度 + 距离组合，自动决定能走到哪一步，并给出结果。
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

    // ============================================================
    // 1) 固定的“超速违法”完整流程
    // ============================================================
    @GetMapping("/api/debug/simulate")
    public Map<String, Object> simulateOneViolation() {

        RadarData radarData = RadarData.builder()
                .distanceMiles(0.0)  // 接近设备
                .speedMph(50.0)      // 明显超速（> 44 mph 阈值）
                .build();

        Optional<RadarSample> maybeSample = radarDataCollectorService.processRadarData(radarData);
        if (maybeSample.isEmpty()) {
            return Map.of(
                    "success", false,
                    "stage", "RadarDataCollector",
                    "reason", "Sample filtered out by RadarDataCollector."
            );
        }
        RadarSample sample = maybeSample.get();

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

        LedCommand ledCommand = ledDisplayControllerService.buildLedCommand(speedStatus);

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

        byte[] fakeImage = "fakeImageBytes".getBytes();
        CameraData rawCameraData = CameraData.builder()
                .imageBytes(fakeImage)
                .timestampMillis(System.currentTimeMillis())
                .build();

        Optional<CameraData> maybeProcessedFrame = cameraDataCollectorService.processCameraFrame(rawCameraData);
        if (maybeProcessedFrame.isEmpty()) {
            return Map.of(
                    "success", false,
                    "stage", "CameraDataCollector",
                    "reason", "CameraDataCollector rejected the frame."
            );
        }
        CameraData processedFrame = maybeProcessedFrame.get();

        Optional<PlateInfo> maybePlate = anprProcessorService.processFrame(processedFrame);
        if (maybePlate.isEmpty()) {
            return Map.of(
                    "success", false,
                    "stage", "AnprProcessor",
                    "reason", "ANPR did not produce PlateInfo."
            );
        }
        PlateInfo plateInfo = maybePlate.get();

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

        UploadStatus uploadStatus = backendUplinkControllerService.uploadViolationRecord(record);

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

    // ============================================================
    // 2) 正常行驶（未超速）流程测试
    // ============================================================
    @GetMapping("/api/debug/simulateNormal")
    public Map<String, Object> simulateNormalDriving(
            @RequestParam(name = "speedMph", defaultValue = "30.0") double speedMph
    ) {

        RadarData radarData = RadarData.builder()
                .distanceMiles(0.0)
                .speedMph(speedMph)
                .build();

        Optional<RadarSample> maybeSample = radarDataCollectorService.processRadarData(radarData);
        if (maybeSample.isEmpty()) {
            return Map.of(
                    "success", false,
                    "stage", "RadarDataCollector",
                    "reason", "Sample filtered out by RadarDataCollector."
            );
        }
        RadarSample sample = maybeSample.get();

        SpeedStatus speedStatus = speedViolationControllerService.buildSpeedStatus(sample);
        Optional<SpeedContext> maybeCtx = speedViolationControllerService.buildOverspeedContext(sample);

        LedCommand ledCommand = ledDisplayControllerService.buildLedCommand(speedStatus);

        return Map.of(
                "success", true,
                "mode", "normal-driving",
                "inputSpeedMph", speedMph,
                "overspeedContextPresent", maybeCtx.isPresent(),  // 正常情况下应为 false
                "ledMessage", ledCommand.getMessage(),
                "speedStatus", speedStatus
        );
    }

    // ============================================================
    // 3) 速度 + 距离组合测试
    // ============================================================
    /**
     * 组合测试：给定 speedMph + distanceMiles，查看系统会走到哪一步。
     *
     * 示例：
     *   - /api/debug/simulateCase?speedMph=30&distanceMiles=0
     *       => 正常行驶 + 在抓拍区，should be no overspeed, 不触发证据链路
     *
     *   - /api/debug/simulateCase?speedMph=50&distanceMiles=0
     *       => 超速 + 在抓拍区，完整违法流程
     *
     *   - /api/debug/simulateCase?speedMph=50&distanceMiles=0.02
     *       => 超速 + 但可能远离抓拍窗口（取决于你 RadarDataCollector 的距离规则）
     */
    @GetMapping("/api/debug/simulateCase")
    public Map<String, Object> simulateCustomCase(
            @RequestParam(name = "speedMph") double speedMph,
            @RequestParam(name = "distanceMiles") double distanceMiles
    ) {

        // 1. 构造 RadarData
        RadarData radarData = RadarData.builder()
                .distanceMiles(distanceMiles)
                .speedMph(speedMph)
                .build();

        // 2. Radar Data Collector
        Optional<RadarSample> maybeSample = radarDataCollectorService.processRadarData(radarData);
        if (maybeSample.isEmpty()) {
            return Map.of(
                    "success", true,   // 对测试来说，这也是一种“预期行为”
                    "mode", "custom-case",
                    "stage", "RadarDataCollector",
                    "reason", "Sample filtered out by RadarDataCollector.",
                    "inputSpeedMph", speedMph,
                    "inputDistanceMiles", distanceMiles
            );
        }
        RadarSample sample = maybeSample.get();

        // 3. Speed Violation Controller
        SpeedStatus speedStatus = speedViolationControllerService.buildSpeedStatus(sample);
        Optional<SpeedContext> maybeCtx = speedViolationControllerService.buildOverspeedContext(sample);
        boolean overspeed = maybeCtx.isPresent();

        // 4. LED 显示
        LedCommand ledCommand = ledDisplayControllerService.buildLedCommand(speedStatus);

        // 如果没有 overspeed，直接返回，不触发后续链路
        if (!overspeed) {
            return Map.of(
                    "success", true,
                    "mode", "custom-case-no-overspeed",
                    "inputSpeedMph", speedMph,
                    "inputDistanceMiles", distanceMiles,
                    "overspeed", false,
                    "captureActive", null,
                    "violationRecordPresent", false,
                    "uploadSuccess", false,
                    "ledMessage", ledCommand.getMessage(),
                    "speedStatus", speedStatus
            );
        }

        // 5. overspeed 情况 -> 调用 ECC
        SpeedContext speedContext = maybeCtx.get();
        EvidenceCaptureResult eccResult = evidenceCaptureControllerService.handleSpeedContext(speedContext);
        Boolean captureActive = eccResult.getCaptureActive();
        SpeedContext ctxForPackager = eccResult.getSpeedContext();

        // 如果 ECC 没有激活捕捉（例如距离 <= -20 或 >= 20），则不再继续证据链路
        if (!Boolean.TRUE.equals(captureActive) || ctxForPackager == null) {
            return Map.of(
                    "success", true,
                    "mode", "custom-case-overspeed-no-capture",
                    "inputSpeedMph", speedMph,
                    "inputDistanceMiles", distanceMiles,
                    "overspeed", true,
                    "captureActive", captureActive,
                    "violationRecordPresent", false,
                    "uploadSuccess", false,
                    "ledMessage", ledCommand.getMessage(),
                    "speedStatus", speedStatus
            );
        }

        // 6. 进入完整证据链路：Camera -> ANPR -> Packager -> Uplink
        byte[] fakeImage = "fakeImageBytes".getBytes();
        CameraData rawCameraData = CameraData.builder()
                .imageBytes(fakeImage)
                .timestampMillis(System.currentTimeMillis())
                .build();

        Optional<CameraData> maybeProcessedFrame = cameraDataCollectorService.processCameraFrame(rawCameraData);
        if (maybeProcessedFrame.isEmpty()) {
            return Map.of(
                    "success", true,
                    "mode", "custom-case-overspeed-camera-rejected",
                    "inputSpeedMph", speedMph,
                    "inputDistanceMiles", distanceMiles,
                    "overspeed", true,
                    "captureActive", captureActive,
                    "violationRecordPresent", false,
                    "uploadSuccess", false,
                    "ledMessage", ledCommand.getMessage()
            );
        }
        CameraData processedFrame = maybeProcessedFrame.get();

        Optional<PlateInfo> maybePlate = anprProcessorService.processFrame(processedFrame);
        if (maybePlate.isEmpty()) {
            return Map.of(
                    "success", true,
                    "mode", "custom-case-overspeed-no-plate",
                    "inputSpeedMph", speedMph,
                    "inputDistanceMiles", distanceMiles,
                    "overspeed", true,
                    "captureActive", captureActive,
                    "violationRecordPresent", false,
                    "uploadSuccess", false,
                    "ledMessage", ledCommand.getMessage()
            );
        }
        PlateInfo plateInfo = maybePlate.get();

        Optional<ViolationRecord> maybeRecord =
                evidenceCollectorAndPackagerService.buildViolationRecord(
                        ctxForPackager,
                        plateInfo,
                        processedFrame
                );

        if (maybeRecord.isEmpty()) {
            return Map.of(
                    "success", true,
                    "mode", "custom-case-overspeed-no-record",
                    "inputSpeedMph", speedMph,
                    "inputDistanceMiles", distanceMiles,
                    "overspeed", true,
                    "captureActive", captureActive,
                    "violationRecordPresent", false,
                    "uploadSuccess", false,
                    "ledMessage", ledCommand.getMessage()
            );
        }
        ViolationRecord record = maybeRecord.get();

        UploadStatus uploadStatus = backendUplinkControllerService.uploadViolationRecord(record);

        return Map.ofEntries(
                Map.entry("success", true),
                Map.entry("mode", "custom-case-overspeed-full"),
                Map.entry("inputSpeedMph", speedMph),
                Map.entry("inputDistanceMiles", distanceMiles),
                Map.entry("overspeed", true),
                Map.entry("captureActive", captureActive),
                Map.entry("violationRecordPresent", true),
                Map.entry("uploadSuccess", uploadStatus.isSuccess()),
                Map.entry("ledMessage", ledCommand.getMessage()),
                Map.entry("violationRecord", record),
                Map.entry("uploadStatus", uploadStatus)
        );

    }
}
