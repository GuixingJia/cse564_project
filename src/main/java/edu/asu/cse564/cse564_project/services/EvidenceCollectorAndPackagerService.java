package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.CameraData;
import edu.asu.cse564.cse564_project.domain.PlateInfo;
import edu.asu.cse564.cse564_project.domain.SpeedContext;
import edu.asu.cse564.cse564_project.domain.ViolationRecord;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/*
 * EvidenceCollectorAndPackagerService
 *
 * Aggregates data from multiple CPS components to create a complete
 * ViolationRecord. It receives contextual speed information, ANPR
 * results, and a captured image frame, then combines them into a
 * structured violation record for backend upload.
 *
 * Only overspeed events with valid inputs will produce a record.
 * Missing inputs or non-overspeed events result in Optional.empty().
 */
@Service
public class EvidenceCollectorAndPackagerService {

    /*
     * Creates a ViolationRecord from SpeedContext, PlateInfo, and CameraData.
     * Returns Optional.empty() if any input is missing or if the event is not overspeed.
     */
    public Optional<ViolationRecord> buildViolationRecord(
            SpeedContext speedContext,
            PlateInfo plateInfo,
            CameraData cameraData
    ) {
        // Reject if any required input is missing
        if (speedContext == null || plateInfo == null || cameraData == null) {
            return Optional.empty();
        }

        // Defensive check — ensures only overspeed cases generate evidence
        if (!speedContext.isOverspeed()) {
            return Optional.empty();
        }

        // Generate a unique local violation ID
        String violationId = UUID.randomUUID().toString();

        // Assemble the final violation record
        ViolationRecord record = ViolationRecord.builder()
                .violationId(violationId)
                .plateNumber(plateInfo.getPlateNumber())
                .speedMph(speedContext.getSpeedMph())
                .distanceMiles(speedContext.getDistanceMiles())
                .distanceMeters(speedContext.getDistanceMeters())
                .timestampMillis(speedContext.getTimestampMillis())
                .targetId(speedContext.getTargetId())
                .imageBytes(cameraData.getImageBytes())
                .build();

        return Optional.of(record);
    }
}
