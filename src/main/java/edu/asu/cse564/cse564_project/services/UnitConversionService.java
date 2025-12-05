package edu.asu.cse564.cse564_project.services;

import org.springframework.stereotype.Service;

/*
 * UnitConversionService
 *
 * Provides centralized conversions between U.S. customary units
 * (mile, mph) and SI units (meter, m/s).
 * Used throughout the CPS pipeline to ensure consistent internal calculations.
 */
@Service
public class UnitConversionService {

    // Number of meters in one mile
    public static final double METERS_PER_MILE = 1609.344;

    // Number of seconds in one hour
    public static final double SECONDS_PER_HOUR = 3600.0;

    // Convert distance from miles to meters
    public double milesToMeters(double miles) {
        return miles * METERS_PER_MILE;
    }

    // Convert distance from meters to miles
    public double metersToMiles(double meters) {
        return meters / METERS_PER_MILE;
    }

    // Convert speed from mph to meters per second
    public double mphToMetersPerSecond(double mph) {
        double milesPerSecond = mph / SECONDS_PER_HOUR;
        return milesPerSecond * METERS_PER_MILE;
    }

    // Convert speed from meters per second to mph
    public double metersPerSecondToMph(double metersPerSecond) {
        double milesPerSecond = metersPerSecond / METERS_PER_MILE;
        return milesPerSecond * SECONDS_PER_HOUR;
    }
}
