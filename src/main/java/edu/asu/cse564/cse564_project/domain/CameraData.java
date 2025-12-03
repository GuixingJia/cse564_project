package edu.asu.cse564.cse564_project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Camera frame data.
 * Uses timestamp in milliseconds; does not depend on U.S. units.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraData {

    private byte[] imageBytes;

    private long timestampMillis;
}