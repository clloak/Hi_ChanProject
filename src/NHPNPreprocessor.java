/**
 * process NHPN JSON and create the lookup CSV
 */
/** One-time developer tool: creates the precision-5 I-90 geohash lookup CSV. */
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NHPNPreprocessor {
    // Sampling more often than the approximate 2-mile cell length helps catch cells
    // crossed between NHPN geometry vertices.
    private static final double SAMPLE_SPACING_MILES = 0.5;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java NHPNPreprocessor <NHPN.json> <lookup.csv>");
            return;
        }
        try {
            int count = createLookup(Path.of(args[0]), Path.of(args[1]));
            System.out.println("Lookup created. Rows written: " + count);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    public static int createLookup(Path input, Path output) throws IOException {
        Object root = JSONParser.parse(Files.readString(input));
        List<?> features = getFeatures(root);
        List<String> rows = new ArrayList<>();
        rows.add("GEOHASH,ROUTE,STFIPS,STATE,BEGMP,ENDMP,MILEPOST");

        for (Object item : features) {
            if (!(item instanceof Map<?, ?> feature)) continue;
            Map<?, ?> properties = asMap(feature.get("properties"));
            String route = value(first(properties.get("SIGN1"), feature.get("SIGN1")));
            String fips = value(first(properties.get("STFIPS"), feature.get("STFIPS")));
            String state = HighwaySegment.stateFromFips(fips);
            Double begin = number(first(properties.get("BEGMP"), feature.get("BEGMP")));
            Double end = number(first(properties.get("ENDMP"), feature.get("ENDMP")));
            Map<?, ?> geometry = asMap(feature.get("geometry"));
            Object rawCoordinates = geometry.get("coordinates");

            if (!"I90".equalsIgnoreCase(route) || !(state.equals("WA") || state.equals("ID"))
                    || begin == null || end == null || rawCoordinates == null) continue;

            List<Point> points = new ArrayList<>();
            collectPoints(rawCoordinates, points);
            if (points.size() < 2) continue;

            Map<String, MilepostSamples> byHash = sampleFeature(points, begin, end);
            for (Map.Entry<String, MilepostSamples> entry : byHash.entrySet()) {
                double representative = entry.getValue().average();
                rows.add(csv(entry.getKey()) + "," + csv(route) + "," + csv(fips) + ","
                        + csv(state) + "," + begin + "," + end + "," + representative);
            }
        }

        if (rows.size() == 1) throw new IOException("No usable I-90 records for Washington or Idaho were found.");
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            for (String row : rows) { writer.write(row); writer.newLine(); }
        }
        return rows.size() - 1;
    }

    /** Preserves road order and estimates milepost for each cell along the geometry. */
    private static Map<String, MilepostSamples> sampleFeature(List<Point> points, double begin, double end) {
        double[] cumulative = new double[points.size()];
        for (int i = 1; i < points.size(); i++)
            cumulative[i] = cumulative[i - 1] + distanceMiles(points.get(i - 1), points.get(i));
        double total = cumulative[points.size() - 1];
        Map<String, MilepostSamples> result = new LinkedHashMap<>();
        if (total == 0) {
            add(result, points.get(0), begin);
            return result;
        }

        for (int i = 1; i < points.size(); i++) {
            Point a = points.get(i - 1), b = points.get(i);
            double length = cumulative[i] - cumulative[i - 1];
            int steps = Math.max(1, (int) Math.ceil(length / SAMPLE_SPACING_MILES));
            for (int step = 0; step <= steps; step++) {
                double fraction = (double) step / steps;
                Point sample = new Point(a.lon + (b.lon - a.lon) * fraction,
                        a.lat + (b.lat - a.lat) * fraction);
                double along = cumulative[i - 1] + length * fraction;
                double milepost = begin + (end - begin) * (along / total);
                add(result, sample, milepost);
            }
        }
        return result;
    }

    private static void add(Map<String, MilepostSamples> map, Point point, double milepost) {
        String hash = GeoHash.encode(point.lat, point.lon, GeoHash.PRECISION);
        map.computeIfAbsent(hash, ignored -> new MilepostSamples()).add(milepost);
    }
    private static double distanceMiles(Point a, Point b) {
        double radius = 3958.8;
        double lat1 = Math.toRadians(a.lat), lat2 = Math.toRadians(b.lat);
        double dLat = Math.toRadians(b.lat - a.lat), dLon = Math.toRadians(b.lon - a.lon);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * radius * Math.asin(Math.min(1, Math.sqrt(h)));
    }
    private static void collectPoints(Object value, List<Point> points) {
        if (!(value instanceof List<?> list)) return;
        if (list.size() >= 2 && list.get(0) instanceof Number && list.get(1) instanceof Number) {
            // GeoJSON order is [longitude, latitude].
            double lon = ((Number) list.get(0)).doubleValue();
            double lat = ((Number) list.get(1)).doubleValue();
            if (Double.isFinite(lon) && Double.isFinite(lat) && lon >= -180 && lon <= 180 && lat >= -90 && lat <= 90)
                points.add(new Point(lon, lat));
            return;
        }
        for (Object child : list) collectPoints(child, points);
    }
    private static List<?> getFeatures(Object root) throws IOException {
        if (root instanceof Map<?, ?> map && map.get("features") instanceof List<?> list) return list;
        throw new IOException("Expected a GeoJSON object containing a features array.");
    }
    private static Map<?, ?> asMap(Object value) { return value instanceof Map<?, ?> map ? map : Map.of(); }
    private static Object first(Object a, Object b) { return a != null ? a : b; }
    private static String value(Object value) { return value == null ? "" : value.toString().trim(); }
    private static Double number(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        try { return value == null ? null : Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) { return null; }
    }
    private static String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }
    private static final class Point {
        final double lon, lat;
        Point(double lon, double lat) { this.lon = lon; this.lat = lat; }
    }
    private static final class MilepostSamples {
        double sum; int count;
        void add(double value) { sum += value; count++; }
        double average() { return sum / count; }
    }
}
