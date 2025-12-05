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
 * Provides debugging and demonstration endpoints that exercise the full
 * CPS enforcement pipeline end-to-end:
 *
 *   RadarData -> RadarDataCollector -> RadarSample
 *     -> SpeedViolationController (SpeedStatus + SpeedContext)
 *     -> LEDDisplayController (LedCommand)
 *     -> EvidenceCaptureController (EvidenceCaptureResult)
 *     -> CameraDataCollector -> CameraData
 *     -> ANPR (PlateInfo)
 *     -> EvidenceCollectorAndPackager (ViolationRecord)
 *     -> BackendUplinkController (UploadStatus)
 *
 * The controller also exposes a parametric endpoint to test different
 * speed and distance combinations and see how they map into zones:
 *
 *   d <= -150m              : OUT_OF_RANGE_BEFORE
 *   -150m < d <= -90m       : COARSE_ONLY
 *   -90m < d <= -20m        : MONITOR_ONLY
 *   -20m < d < 20m          : CAPTURE_WINDOW
 *   20m <= d <= 90m         : LEAVING_STOP_CAPTURE
 *   d > 90m                 : OUT_OF_RANGE_AFTER
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
    private final UnitConversionService unitConversionService;

    public DebugSimulationController(
            RadarDataCollectorService radarDataCollectorService,
            SpeedViolationControllerService speedViolationControllerService,
            LedDisplayControllerService ledDisplayControllerService,
            EvidenceCaptureControllerService evidenceCaptureControllerService,
            CameraDataCollectorService cameraDataCollectorService,
            AnprProcessorService anprProcessorService,
            EvidenceCollectorAndPackagerService evidenceCollectorAndPackagerService,
            BackendUplinkControllerService backendUplinkControllerService,
            UnitConversionService unitConversionService
    ) {
        this.radarDataCollectorService = radarDataCollectorService;
        this.speedViolationControllerService = speedViolationControllerService;
        this.ledDisplayControllerService = ledDisplayControllerService;
        this.evidenceCaptureControllerService = evidenceCaptureControllerService;
        this.cameraDataCollectorService = cameraDataCollectorService;
        this.anprProcessorService = anprProcessorService;
        this.evidenceCollectorAndPackagerService = evidenceCollectorAndPackagerService;
        this.backendUplinkControllerService = backendUplinkControllerService;
        this.unitConversionService = unitConversionService;
    }

    // ============================================================
    // 1) Fixed overspeed scenario – full violation pipeline sanity check
    // ============================================================
    @GetMapping("/api/debug/simulate")
    public Map<String, Object> simulateOneViolation() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Near the device (0 m) with overspeed
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
    // 2) Normal driving test (no overspeed)
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
    // 3) Parametric speed + distance test (core debugging endpoint)
    // ============================================================
    @GetMapping("/api/debug/simulateCase")
    public Map<String, Object> simulateCustomCase(
            @RequestParam(name = "speedMph") double speedMph,
            @RequestParam(name = "distanceMiles") double distanceMiles
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Convert miles to meters using the shared conversion service
        double distanceMeters = unitConversionService.milesToMeters(distanceMiles);

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
        boolean overspeedContextPresent = maybeCtx.isPresent();

        LedCommand ledCommand = ledDisplayControllerService.buildLedCommand(speedStatus);

        result.put("stage", "SpeedViolationController");
        result.put("speedStatus", speedStatus);
        result.put("isOverspeed", overspeedContextPresent);
        result.put("ledMessage", ledCommand.getMessage());

        // No overspeed context or coarse-only region → evidence pipeline not triggered
        if (!overspeedContextPresent) {
            result.put("reason", "Not overspeed or still in coarse-only zone; ECC and evidence pipeline not triggered.");
            result.put("captureActive", null);
            result.put("violationRecordPresent", false);
            result.put("uploadSuccess", false);
            return result;
        }

        // 3) EvidenceCaptureController
        SpeedContext speedContext = maybeCtx.get();
        EvidenceCaptureResult eccResult = evidenceCaptureControllerService.handleSpeedContext(speedContext);
        Boolean captureActive = eccResult.getCaptureActive();
        SpeedContext ctxForPackager = eccResult.getSpeedContext();

        result.put("stage", "EvidenceCaptureController");
        result.put("captureActive", captureActive);

        // Before capture window
        if (captureActive == null && ctxForPackager == null) {
            result.put("reason", "Overspeed but before capture window; ECC does not change capture state.");
            result.put("violationRecordPresent", false);
            result.put("uploadSuccess", false);
            return result;
        }

        // After capture window (leaving side)
        if (Boolean.FALSE.equals(captureActive) && ctxForPackager == null) {
            result.put("reason", "Overspeed but outside capture window on leaving side; ECC stops capture.");
            result.put("violationRecordPresent", false);
            result.put("uploadSuccess", false);
            return result;
        }

        // Inside capture window → full evidence pipeline
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

        // Fallback: unexpected ECC combination
        result.put("reason", "Unexpected ECC state combination.");
        result.put("violationRecordPresent", false);
        result.put("uploadSuccess", false);
        return result;
    }
}
