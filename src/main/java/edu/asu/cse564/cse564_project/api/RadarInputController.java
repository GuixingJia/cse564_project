package edu.asu.cse564.cse564_project.api;

import edu.asu.cse564.cse564_project.domain.*;
import edu.asu.cse564.cse564_project.services.*;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/*
 * RadarInputController
 *
 * Provides a realistic POST-based entrypoint for radar sensor input.
 * This controller simulates the complete CPS enforcement pipeline:
 *
 *   RadarData (POST)
 *     → RadarDataCollector
 *     → SpeedViolationController (SpeedStatus + SpeedContext)
 *     → LEDDisplayController
 *     → EvidenceCaptureController
 *     → CameraDataCollector
 *     → ANPR Processor
 *     → EvidenceCollectorAndPackager
 *     → BackendUplinkController
 *
 * The endpoint returns a detailed JSON trace describing how the input
 * propagates through each stage of the pipeline.
 */
@RestController
@RequestMapping("/api/radar")
public class RadarInputController {

    private final RadarDataCollectorService radarDataCollectorService;
    private final SpeedViolationControllerService speedViolationControllerService;
    private final LedDisplayControllerService ledDisplayControllerService;
    private final EvidenceCaptureControllerService evidenceCaptureControllerService;
    private final CameraDataCollectorService cameraDataCollectorService;
    private final AnprProcessorService anprProcessorService;
    private final EvidenceCollectorAndPackagerService evidenceCollectorAndPackagerService;
    private final BackendUplinkControllerService backendUplinkControllerService;

    public RadarInputController(
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

    /*
     * POST /api/radar/sample
     *
     * Ingests a radar measurement and runs it through the full CPS chain.
     */
    @PostMapping("/sample")
    public Map<String, Object> ingestRadarSample(@RequestBody RadarData radarData) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("input", radarData);

        // -------------------------------
        // 1) Radar Data Collector
        // -------------------------------
        Optional<RadarSample> maybeSample = radarDataCollectorService.processRadarData(radarData);
        if (maybeSample.isEmpty()) {
            result.put("accepted", false);
            result.put("stage", "RadarDataCollector");
            result.put("reason", "RadarDataCollector rejected the sample (out of range).");
            return result;
        }
        RadarSample sample = maybeSample.get();
        result.put("radarSample", sample);

        // -------------------------------
        // 2) Speed Violation Controller
        // -------------------------------
        SpeedStatus speedStatus = speedViolationControllerService.buildSpeedStatus(sample);
        Optional<SpeedContext> maybeCtx =
                speedViolationControllerService.buildOverspeedContext(sample);

        result.put("speedStatus", speedStatus);
        result.put("overspeedContextPresent", maybeCtx.isPresent());

        // LED always receives updates
        LedCommand ledCommand = ledDisplayControllerService.buildLedCommand(speedStatus);
        result.put("ledMessage", ledCommand.getMessage());

        // If no overspeed context exists, evidence pipeline is not triggered
        if (maybeCtx.isEmpty()) {
            result.put("accepted", true);
            result.put("stage", "SpeedViolationController");
            result.put("reason", "No SpeedContext generated (not overspeed or coarse-only region).");
            result.put("captureActive", null);
            return result;
        }

        SpeedContext speedContext = maybeCtx.get();

        // -------------------------------
        // 3) Evidence Capture Controller
        // -------------------------------
        EvidenceCaptureResult eccResult =
                evidenceCaptureControllerService.handleSpeedContext(speedContext);

        result.put("captureActive", eccResult.getCaptureActive());
        result.put("stage", "EvidenceCaptureController");

        // Before capture window
        if (eccResult.getCaptureActive() == null && eccResult.getSpeedContext() == null) {
            result.put("reason", "Overspeed but before capture window.");
            return result;
        }

        // Leaving capture window
        if (Boolean.FALSE.equals(eccResult.getCaptureActive()) &&
                eccResult.getSpeedContext() == null) {
            result.put("reason", "Overspeed but outside capture window; ECC stopped capture.");
            return result;
        }

        SpeedContext ctxForPackager = eccResult.getSpeedContext();

        // -------------------------------
        // 4) Camera frame (simulated)
        // -------------------------------
        byte[] fakeImage = "fakeImageBytes".getBytes();
        CameraData rawFrame = CameraData.builder()
                .imageBytes(fakeImage)
                .timestampMillis(System.currentTimeMillis())
                .build();

        Optional<CameraData> maybeFrame =
                cameraDataCollectorService.processCameraFrame(rawFrame);

        if (maybeFrame.isEmpty()) {
            result.put("stage", "CameraDataCollector");
            result.put("reason", "CameraData rejected.");
            return result;
        }
        CameraData processedFrame = maybeFrame.get();

        // -------------------------------
        // 5) ANPR Processor
        // -------------------------------
        Optional<PlateInfo> maybePlate = anprProcessorService.processFrame(processedFrame);
        if (maybePlate.isEmpty()) {
            result.put("stage", "ANPR");
            result.put("reason", "No PlateInfo produced.");
            return result;
        }
        PlateInfo plateInfo = maybePlate.get();

        // -------------------------------
        // 6) Evidence Collector & Packager
        // -------------------------------
        Optional<ViolationRecord> maybeRecord =
                evidenceCollectorAndPackagerService.buildViolationRecord(
                        ctxForPackager,
                        plateInfo,
                        processedFrame);

        if (maybeRecord.isEmpty()) {
            result.put("stage", "EvidenceCollectorAndPackager");
            result.put("reason", "No ViolationRecord generated.");
            return result;
        }
        ViolationRecord record = maybeRecord.get();
        result.put("violationRecord", record);

        // -------------------------------
        // 7) Backend Upload
        // -------------------------------
        UploadStatus uploadStatus =
                backendUplinkControllerService.uploadViolationRecord(record);

        result.put("uploadStatus", uploadStatus);
        result.put("stage", "BackendUplinkController");
        result.put("accepted", true);
        result.put("reason", "Full evidence pipeline executed successfully.");

        return result;
    }
}
