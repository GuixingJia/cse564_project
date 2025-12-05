package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.CameraData;
import edu.asu.cse564.cse564_project.domain.PlateInfo;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/*
 * AnprProcessorService
 *
 * Simulates an Automatic Number Plate Recognition (ANPR) module.
 * This mock implementation makes several simplifications for testing:
 *   - Plate numbers are selected from a small, hardcoded list.
 *   - Any non-null camera frame is considered valid input.
 *   - No image processing or OCR is performed.
 *
 * In a real system, this service would run OCR models and return
 * detection confidence, bounding boxes, and more detailed metadata.
 */
@Service
public class AnprProcessorService {

    private final Random random = new Random();

    // Hardcoded mock plate numbers used for simulation
    private static final List<String> MOCK_PLATE_NUMBERS = Arrays.asList(
            "MGE-4592",
            "N4M-1249",
            "K2P-9087"
    );

    /*
     * Produces a PlateInfo object based on a randomly selected mock plate.
     * Returns Optional.empty() if no input frame is provided.
     */
    public Optional<PlateInfo> processFrame(CameraData cameraData) {
        if (cameraData == null) {
            return Optional.empty();
        }

        // Choose one plate number randomly
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
