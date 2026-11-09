/**
 * match geohashes to highway information
 */
/** Loads the prepared lookup CSV and retains every highway candidate for each geohash. */
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GeohashLookup {
    public static final int GEOHASH_PRECISION = GeoHash.PRECISION;
    private final Map<String, List<HighwaySegment>> segmentsByHash = new HashMap<>();

    public GeohashLookup(Path lookupPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(lookupPath)) {
            String line = reader.readLine();
            if (line == null) throw new IOException("Lookup CSV is empty.");
            List<String> header = parseCsv(line);
            Map<String, Integer> columns = new HashMap<>();
            for (int i = 0; i < header.size(); i++)
                columns.put(header.get(i).replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT), i);
            for (String required : List.of("geohash", "route", "stfips", "state", "begmp", "endmp", "milepost"))
                if (!columns.containsKey(required)) throw new IOException("Lookup CSV is missing column: " + required);

            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                try {
                    List<String> values = parseCsv(line);
                    String hash = get(values, columns, "geohash").toLowerCase(Locale.ROOT).trim();
                    String route = get(values, columns, "route").trim();
                    String fips = get(values, columns, "stfips").trim();
                    String state = get(values, columns, "state").trim();
                    double begin = Double.parseDouble(get(values, columns, "begmp"));
                    double end = Double.parseDouble(get(values, columns, "endmp"));
                    double milepost = Double.parseDouble(get(values, columns, "milepost"));
                    if (hash.length() != GEOHASH_PRECISION || !Double.isFinite(begin)
                            || !Double.isFinite(end) || !Double.isFinite(milepost))
                        throw new IllegalArgumentException("Invalid geohash or milepost value");
                    HighwaySegment segment = new HighwaySegment(route, fips, state, begin, end, milepost, hash);
                    // Do not overwrite: a cell can be shared by multiple NHPN features.
                    segmentsByHash.computeIfAbsent(hash, ignored -> new ArrayList<>()).add(segment);
                } catch (RuntimeException e) {
                    throw new IOException("Invalid lookup CSV row " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        }
        if (segmentsByHash.isEmpty()) throw new IOException("Lookup CSV contains no usable entries.");
    }

    public List<HighwaySegment> findCandidates(double latitude, double longitude) {
        Set<String> hashes = GeoHash.neighbors(latitude, longitude, GEOHASH_PRECISION);
        List<HighwaySegment> result = new ArrayList<>();
        for (String hash : hashes) {
            List<HighwaySegment> matches = segmentsByHash.get(hash);
            if (matches != null) result.addAll(matches);
        }
        return result;
    }

    /** Selects the candidate nearest the previous milepost, preferring the same state. */
    public HighwaySegment findBest(double latitude, double longitude, HighwaySegment previous) {
        List<HighwaySegment> candidates = findCandidates(latitude, longitude);
        if (candidates.isEmpty()) return null;
        if (previous == null) return candidates.get(0);
        HighwaySegment best = null;
        double bestScore = Double.MAX_VALUE;
        for (HighwaySegment candidate : candidates) {
            double score = Math.abs(candidate.getRepresentativeMilepost() - previous.getRepresentativeMilepost());
            if (!candidate.getState().equals(previous.getState())) score += 1000.0;
            if (!candidate.getRoute().equals(previous.getRoute())) score += 10000.0;
            if (score < bestScore) { bestScore = score; best = candidate; }
        }
        return best;
    }

    private static String get(List<String> values, Map<String, Integer> columns, String name) {
        int index = columns.get(name);
        if (index >= values.size()) throw new IllegalArgumentException("Missing value for " + name);
        return values.get(index).trim();
    }
    private static List<String> parseCsv(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { field.append('"'); i++; }
                else if (ch == '"') quoted = false;
                else field.append(ch);
            } else if (ch == ',' ) { fields.add(field.toString()); field.setLength(0); }
            else if (ch == '"' && field.length() == 0) quoted = true;
            else field.append(ch);
        }
        if (quoted) throw new IOException("CSV row contains an unclosed quote.");
        fields.add(field.toString());
        return fields;
    }
}
