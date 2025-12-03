package edu.asu.cse564.cse564_project.api;

import edu.asu.cse564.cse564_project.domain.*;
import edu.asu.cse564.cse564_project.services.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/*
 * DebugSimulationController
 *
 * 调试 / 演示用控制器，用来串联整个 CPS 监控执法流水线：
 *
 *   RadarData -> RadarDataCollector -> RadarSample
 *   -> SpeedViolationController (SpeedStatus + SpeedContext)
 *   -> LEDDisplayController (LedCommand)
 *   -> EvidenceCaptureController (EvidenceCaptureResult: captureActive + SpeedContext)
 *   -> CameraDataCollector -> CameraData
 *   -> ANPR (PlateInfo)
 *   -> EvidenceCollectorAndPackager (ViolationRecord)
 *   -> BackendUplinkController (UploadStatus)
 *
 * 物理距离（单位：米）区间设定：
 *
 *   d <= -150m                : OUT_OF_RANGE_BEFORE  → Radar 丢弃，不进入后续逻辑
 *   -150m < d <= -90m         : COARSE_ONLY          → SVC 只做粗测超速，不向 ECC 输出上下文
 *   -90m < d <= -20m          : MONITOR_ONLY         → 正式测速监测区，超速则给 LED & ECC
 *   -20m < d < 20m            : CAPTURE_WINDOW       → 抓拍取证窗口，ECC 抓拍
 *   d >= 20m 且 d <= 90m      : LEAVING_STOP_CAPTURE → 离开抓拍区，ECC 停止抓拍（仅第一次 >20m 的样本会向后传）
 *   d > 90m                   : OUT_OF_RANGE_AFTER   → Radar 丢弃并 reset 状态
 *
 * 提供 3 个测试 API：
 *
 * 1) 固定的“超速完整违法流程”：
 *    GET  http://localhost:8080/api/debug/simulate
 *
 * 2) 正常行驶（可调速度）：
 *    GET  http://localhost:8080/api/debug/simulateNormal
 *    GET  http://localhost:8080/api/debug/simulateNormal?speedMph=42
 *
 * 3) 速度 + 距离组合测试（核心调试接口）：
 *    GET http://localhost:8080/api/debug/simulateCase?speedMph=48&distanceMiles=-0.5
 *    GET  distanceMiles=-0.1      (~ -160m, 应被 Radar 丢弃)
 *    GET  distanceMiles=-0.0621   (~ -100m, 粗测区 COARSE_ONLY)
 *    GET  distanceMiles=-0.0311   (~ -50m, 监测区 MONITOR_ONLY)
 *    GET  distanceMiles=-0.0062   (~ -10m, 抓拍窗口 CAPTURE_WINDOW)
 *    GET  distanceMiles=0         (0m, 抓拍窗口 CAPTURE_WINDOW)
 *    GET  distanceMiles=0.0062    (~ +10m, 抓拍窗口 CAPTURE_WINDOW)
 *    GET  distanceMiles=0.0155    (~ +25m, LEAVING_STOP_CAPTURE → 停止抓拍)
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
    // 1) 固定“超速违法完整流程” sanity check
    // ============================================================
    @GetMapping("/api/debug/simulate")
    public Map<String, Object> simulateOneViolation() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 设备附近（0m）+ 超速
        RadarData radarData = RadarData.builder()
                .distanceMiles(0.0)
                .speedMph(50.0)
                .build();

        Optional<RadarSample> maybeSample = radarDataCollectorService.processRadarData(radarData);
        if (maybeSample.isEmpty()) {
            result.put("success", false);
            result.put("stage", "RadarDataCollector");
            result.put("reason", "Sample filtered out by RadarDataCollector.");
            return result;
        }
        RadarSample sample = maybeSample.get();

        // SpeedViolationController
        SpeedStatus speedStatus = speedViolationControllerService.buildSpeedStatus(sample);
        Optional<SpeedContext> maybeCtx = speedViolationControllerService.buildOverspeedContext(sample);
        if (maybeCtx.isEmpty()) {
            result.put("success", false);
            result.put("stage", "SpeedViolationController");
            result.put("reason", "No overspeed context produced (not overspeed or out of monitor zone).");
            result.put("speedStatus", speedStatus);
            return result;
        }
        SpeedContext speedContext = maybeCtx.get();

        // LED
        LedCommand ledCommand = ledDisplayControllerService.buildLedCommand(speedStatus);

        // ECC
        EvidenceCaptureResult eccResult = evidenceCaptureControllerService.handleSpeedContext(speedContext);
        Boolean captureActive = eccResult.getCaptureActive();
        if (!Boolean.TRUE.equals(captureActive) || eccResult.getSpeedContext() == null) {
            result.put("success", false);
            result.put("stage", "EvidenceCaptureController");
            result.put("reason", "ECC did not activate capture or did not forward context.");
            result.put("captureActive", captureActive);
            result.put("ledMessage", ledCommand.getMessage());
            return result;
        }

        SpeedContext ctxForPackager = eccResult.getSpeedContext();

        // Camera frame (fake)
        byte[] fakeImage = "fakeImageBytes".getBytes();
        CameraData rawCameraData = CameraData.builder()
                .imageBytes(fakeImage)
                .timestampMillis(System.currentTimeMillis())
                .build();

        Optional<CameraData> maybeProcessedFrame = cameraDataCollectorService.processCameraFrame(rawCameraData);
        if (maybeProcessedFrame.isEmpty()) {
            result.put("success", false);
            result.put("stage", "CameraDataCollector");
            result.put("reason", "CameraDataCollector rejected the frame.");
            return result;
        }
        CameraData processedFrame = maybeProcessedFrame.get();

        Optional<PlateInfo> maybePlate = anprProcessorService.processFrame(processedFrame);
        if (maybePlate.isEmpty()) {
            result.put("success", false);
            result.put("stage", "AnprProcessor");
            result.put("reason", "ANPR did not produce PlateInfo.");
            return result;
        }
        PlateInfo plateInfo = maybePlate.get();

        Optional<ViolationRecord> maybeRecord =
                evidenceCollectorAndPackagerService.buildViolationRecord(
                        ctxForPackager,
                        plateInfo,
                        processedFrame
                );

        if (maybeRecord.isEmpty()) {
            result.put("success", false);
            result.put("stage", "EvidenceCollectorAndPackager");
            result.put("reason", "No ViolationRecord produced.");
            return result;
        }
        ViolationRecord record = maybeRecord.get();

        UploadStatus uploadStatus = backendUplinkControllerService.uploadViolationRecord(record);

        result.put("success", true);
        result.put("mode", "overspeed-violation-fixed");
        result.put("ledMessage", ledCommand.getMessage());
        result.put("captureActive", captureActive);
        result.put("plateNumber", plateInfo.getPlateNumber());
        result.put("violationRecord", record);
        result.put("uploadStatus", uploadStatus);
        return result;
    }

    // ============================================================
    // 2) 正常行驶测试（不超速）
    // ============================================================
    @GetMapping("/api/debug/simulateNormal")
    public Map<String, Object> simulateNormalDriving(
            @RequestParam(name = "speedMph", defaultValue = "30.0") double speedMph
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        RadarData radarData = RadarData.builder()
                .distanceMiles(0.0)
                .speedMph(speedMph)
                .build();

        Optional<RadarSample> maybeSample = radarDataCollectorService.processRadarData(radarData);
        if (maybeSample.isEmpty()) {
            result.put("success", false);
            result.put("stage", "RadarDataCollector");
            result.put("reason", "Sample filtered out by RadarDataCollector.");
            return result;
        }
        RadarSample sample = maybeSample.get();

        SpeedStatus speedStatus = speedViolationControllerService.buildSpeedStatus(sample);
        Optional<SpeedContext> maybeCtx = speedViolationControllerService.buildOverspeedContext(sample);

        LedCommand ledCommand = ledDisplayControllerService.buildLedCommand(speedStatus);

        result.put("success", true);
        result.put("mode", "normal-driving");
        result.put("inputSpeedMph", speedMph);
        result.put("overspeedContextPresent", maybeCtx.isPresent());
        result.put("ledMessage", ledCommand.getMessage());
        result.put("speedStatus", speedStatus);
        return result;
    }

    // ============================================================
    // 3) 速度 + 距离组合测试（核心）
    // ============================================================
    @GetMapping("/api/debug/simulateCase")
    public Map<String, Object> simulateCustomCase(
            @RequestParam(name = "speedMph") double speedMph,
            @RequestParam(name = "distanceMiles") double distanceMiles
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        // miles -> meters
        double distanceMeters = distanceMiles * 1609.34;

        final double MIN_VALID_METERS = -150.0;
        final double CAPTURE_WINDOW_HALF_METERS = 20.0;
        final double MAX_VALID_METERS = 90.0;

        String regionCode;
        if (distanceMeters <= MIN_VALID_METERS) {
            regionCode = "OUT_OF_RANGE_BEFORE";
        } else if (distanceMeters <= -90.0) {
            regionCode = "COARSE_ONLY";
        } else if (distanceMeters <= -CAPTURE_WINDOW_HALF_METERS) {
            regionCode = "MONITOR_ONLY";
        } else if (distanceMeters < CAPTURE_WINDOW_HALF_METERS) {
            regionCode = "CAPTURE_WINDOW";
        } else if (distanceMeters <= MAX_VALID_METERS) {
            regionCode = "LEAVING_STOP_CAPTURE";
        } else {
            regionCode = "OUT_OF_RANGE_AFTER";
        }

        result.put("success", true);
        result.put("mode", "custom-case");
        result.put("inputSpeedMph", speedMph);
        result.put("inputDistanceMiles", distanceMiles);
        result.put("distanceMeters", distanceMeters);
        result.put("regionCode", regionCode);

        // 1) RadarDataCollector
        RadarData radarData = RadarData.builder()
                .distanceMiles(distanceMiles)
                .speedMph(speedMph)
                .build();

        Optional<RadarSample> maybeSample = radarDataCollectorService.processRadarData(radarData);
        if (maybeSample.isEmpty()) {
            result.put("stage", "RadarDataCollector");
            result.put("reason", "Sample filtered out by RadarDataCollector.");
            result.put("isOverspeed", null);
            result.put("captureActive", null);
            result.put("violationRecordPresent", false);
            result.put("uploadSuccess", false);
            result.put("ledMessage", null);
            return result;
        }
        RadarSample sample = maybeSample.get();

        // 2) SpeedViolationController
        SpeedStatus speedStatus = speedViolationControllerService.buildSpeedStatus(sample);
        Optional<SpeedContext> maybeCtx = speedViolationControllerService.buildOverspeedContext(sample);
        boolean overspeed = maybeCtx.isPresent();

        LedCommand ledCommand = ledDisplayControllerService.buildLedCommand(speedStatus);

        result.put("stage", "SpeedViolationController");
        result.put("speedStatus", speedStatus);
        result.put("isOverspeed", overspeed);
        result.put("ledMessage", ledCommand.getMessage());

        // 不超速：不进入 ECC / 证据链路
        if (!overspeed) {
            result.put("reason", "Not overspeed or still in coarse-only zone; ECC and evidence pipeline not triggered.");
            result.put("captureActive", null);
            result.put("violationRecordPresent", false);
            result.put("uploadSuccess", false);
            return result;
        }

        // 3) ECC
        SpeedContext speedContext = maybeCtx.get();
        EvidenceCaptureResult eccResult = evidenceCaptureControllerService.handleSpeedContext(speedContext);
        Boolean captureActive = eccResult.getCaptureActive();
        SpeedContext ctxForPackager = eccResult.getSpeedContext();

        result.put("stage", "EvidenceCaptureController");
        result.put("captureActive", captureActive);

        // A: 还未进入抓拍窗口（distance <= -20m）→ ECC 不改状态，不转发上下文
        if (captureActive == null && ctxForPackager == null) {
            result.put("reason", "Overspeed but before capture window; ECC does not change capture state.");
            result.put("violationRecordPresent", false);
            result.put("uploadSuccess", false);
            return result;
        }

        // B: 已离开抓拍区（distance >= 20m）→ ECC 停止抓拍，不转发上下文
        if (Boolean.FALSE.equals(captureActive) && ctxForPackager == null) {
            result.put("reason", "Overspeed but outside capture window on leaving side; ECC stops capture.");
            result.put("violationRecordPresent", false);
            result.put("uploadSuccess", false);
            return result;
        }

        // C: 在抓拍窗内（-20m < d < 20m）→ 完整证据链路
        if (Boolean.TRUE.equals(captureActive) && ctxForPackager != null) {
            result.put("reason", "Overspeed inside capture window; full evidence pipeline triggered.");

            // Camera frame (fake)
            byte[] fakeImage = "fakeImageBytes".getBytes();
            CameraData rawCameraData = CameraData.builder()
                    .imageBytes(fakeImage)
                    .timestampMillis(System.currentTimeMillis())
                    .build();

            Optional<CameraData> maybeProcessedFrame = cameraDataCollectorService.processCameraFrame(rawCameraData);
            if (maybeProcessedFrame.isEmpty()) {
                result.put("stage", "CameraDataCollector");
                result.put("reason", "CameraDataCollector rejected the frame.");
                result.put("violationRecordPresent", false);
                result.put("uploadSuccess", false);
                return result;
            }
            CameraData processedFrame = maybeProcessedFrame.get();

            Optional<PlateInfo> maybePlate = anprProcessorService.processFrame(processedFrame);
            if (maybePlate.isEmpty()) {
                result.put("stage", "AnprProcessor");
                result.put("reason", "ANPR did not produce PlateInfo.");
                result.put("violationRecordPresent", false);
                result.put("uploadSuccess", false);
                return result;
            }
            PlateInfo plateInfo = maybePlate.get();

            Optional<ViolationRecord> maybeRecord =
                    evidenceCollectorAndPackagerService.buildViolationRecord(
                            ctxForPackager,
                            plateInfo,
                            processedFrame
                    );

            if (maybeRecord.isEmpty()) {
                result.put("stage", "EvidenceCollectorAndPackager");
                result.put("reason", "No ViolationRecord produced.");
                result.put("violationRecordPresent", false);
                result.put("uploadSuccess", false);
                return result;
            }
            ViolationRecord record = maybeRecord.get();

            UploadStatus uploadStatus = backendUplinkControllerService.uploadViolationRecord(record);

            result.put("stage", "BackendUplinkController");
            result.put("violationRecordPresent", true);
            result.put("uploadSuccess", uploadStatus.isSuccess());
            result.put("violationRecord", record);
            result.put("uploadStatus", uploadStatus);
            return result;
        }

        // 理论上不会到这里，如果到了说明状态组合不符合预期
        result.put("reason", "Unexpected ECC state combination.");
        result.put("violationRecordPresent", false);
        result.put("uploadSuccess", false);
        return result;
    }
}
