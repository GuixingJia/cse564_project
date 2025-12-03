package edu.asu.cse564.cse564_project.services;

import org.springframework.stereotype.Service;

/*
 * Common unit conversion service used by controllers in the CPS.
 *
 * We use U.S. customary units (mile, mph) as the "external" units
 * (e.g., what radar reports, what LED displays, what backend stores).
 *
 * When internal algorithms need SI units (meters, m/s) for
 * geometry or physics-based calculations, they should call this
 * service to convert units instead of hard-coding constants.
 */
@Service
public class UnitConversionService {

    /* Number of meters in one mile. */
    public static final double METERS_PER_MILE = 1609.344;

    /* Number of seconds in one hour. */
    public static final double SECONDS_PER_HOUR = 3600.0;

    /**
     * Convert distance from miles to meters.
     *
     * @param miles value in miles
     * @return value in meters
     */
    public double milesToMeters(double miles) {
        return miles * METERS_PER_MILE;
    }

    /**
     * Convert distance from meters to miles.
     *
     * @param meters value in meters
     * @return value in miles
     */
    public double metersToMiles(double meters) {
        return meters / METERS_PER_MILE;
    }

    /**
     * Convert speed from miles per hour (mph) to meters per second (m/s).
     *
     * @param mph value in miles per hour
     * @return value in meters per second
     */
    public double mphToMetersPerSecond(double mph) {
        double milesPerSecond = mph / SECONDS_PER_HOUR;
        return milesPerSecond * METERS_PER_MILE;
    }

    /**
     * Convert speed from meters per second (m/s) to miles per hour (mph).
     *
     * @param metersPerSecond value in meters per second
     * @return value in miles per hour
     */
    public double metersPerSecondToMph(double metersPerSecond) {
        double milesPerSecond = metersPerSecond / METERS_PER_MILE;
        return milesPerSecond * SECONDS_PER_HOUR;
    }
}