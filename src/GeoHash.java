/**
 * convert GPS coordinates into geohashes
 */
/** Geohash encoder used by both preprocessing and GPS lookup. */
import java.util.LinkedHashSet;
import java.util.Set;

public final class GeoHash {
    public static final int PRECISION = 5;
    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    private GeoHash() { }

    public static String encode(double latitude, double longitude, int precision) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180
                || precision < 1 || precision > 12) {
            throw new IllegalArgumentException("Invalid coordinate or geohash precision.");
        }
        double minLat = -90, maxLat = 90, minLon = -180, maxLon = 180;
        boolean evenBit = true;
        int bit = 0, character = 0;
        StringBuilder hash = new StringBuilder();
        while (hash.length() < precision) {
            if (evenBit) {
                double middle = (minLon + maxLon) / 2;
                if (longitude >= middle) { character |= 1 << (4 - bit); minLon = middle; }
                else maxLon = middle;
            } else {
                double middle = (minLat + maxLat) / 2;
                if (latitude >= middle) { character |= 1 << (4 - bit); minLat = middle; }
                else maxLat = middle;
            }
            evenBit = !evenBit;
            if (bit == 4) { hash.append(BASE32.charAt(character)); bit = 0; character = 0; }
            else bit++;
        }
        return hash.toString();
    }

    /** Exact cell followed by the eight cells touching it. */
    public static Set<String> neighbors(double latitude, double longitude, int precision) {
        Set<String> result = new LinkedHashSet<>();
        String center = encode(latitude, longitude, precision);
        result.add(center);
        double minLat = -90, maxLat = 90, minLon = -180, maxLon = 180;
        boolean even = true;
        for (int i = 0; i < center.length(); i++) {
            int value = BASE32.indexOf(center.charAt(i));
            for (int mask = 16; mask > 0; mask >>= 1) {
                if (even) {
                    double mid = (minLon + maxLon) / 2;
                    if ((value & mask) != 0) minLon = mid; else maxLon = mid;
                } else {
                    double mid = (minLat + maxLat) / 2;
                    if ((value & mask) != 0) minLat = mid; else maxLat = mid;
                }
                even = !even;
            }
        }
        double height = maxLat - minLat, width = maxLon - minLon;
        double centerLat = (minLat + maxLat) / 2, centerLon = (minLon + maxLon) / 2;
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                if (x == 0 && y == 0) continue;
                double lat = centerLat + y * height, lon = centerLon + x * width;
                if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180)
                    result.add(encode(lat, lon, precision));
            }
        }
        return result;
    }
}
