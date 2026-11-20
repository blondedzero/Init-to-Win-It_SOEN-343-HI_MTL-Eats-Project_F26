package com.shopeasy.Domain.REG.factories;

import java.util.concurrent.ThreadLocalRandom;

public record Coordinates(double latitude, double longitude) {

    private static final double MIN_LAT = 45.41;
    private static final double MAX_LAT = 45.70;
    private static final double MIN_LON = -73.97;
    private static final double MAX_LON = -73.47;
    
    public static Coordinates randomMontrealCoordinates() {
        return new Coordinates(
                ThreadLocalRandom.current().nextDouble(MIN_LAT, MAX_LAT),
                ThreadLocalRandom.current().nextDouble(MIN_LON, MAX_LON));
    }
}
