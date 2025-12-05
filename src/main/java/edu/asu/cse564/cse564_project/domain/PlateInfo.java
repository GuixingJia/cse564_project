package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * PlateInfo
 *
 * Represents the output of an ANPR (Automatic Number Plate Recognition)
 * operation. In this project, the ANPR processing is simulated and the
 * plate number is selected from a predefined list rather than extracted
 * from image data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlateInfo {

    // Recognized (simulated) license plate number
    private String plateNumber;

    // Timestamp when this ANPR result was generated (ms since epoch)
    private long timestampMillis;
}
