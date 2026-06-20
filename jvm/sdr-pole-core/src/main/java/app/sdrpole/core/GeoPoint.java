package app.sdrpole.core;

public record GeoPoint(double latitude, double longitude) {
    public GeoPoint {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90)
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
    }

    public double distanceKmTo(GeoPoint other) {
        double earthKm = 6371.0088;
        double lat1 = Math.toRadians(latitude), lat2 = Math.toRadians(other.latitude);
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(other.longitude - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
