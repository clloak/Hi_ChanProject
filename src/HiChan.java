import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Main command-line application for processing GPS logs into Channel IDs. */
public class HiChan {

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("--help")) {
            printHelp();
            return;
        }

        if (args.length != 3) {
            System.err.println("ERROR: Incorrect number of arguments.");
            printHelp();
            System.exit(1);
        }

        try {
            process(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]));
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printHelp() {
        System.out.println("========================================");
        System.out.println("          Hi_Chan - Team 3");
        System.out.println("========================================");
        System.out.println("Generates Channel IDs for GPS records");
        System.out.println("along I-90 in Washington and Idaho.");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java HiChan <GPS_Log.csv> <Lookup.csv> <Output.csv>");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  GPS_Log.csv  - Input GPS log file");
        System.out.println("  Lookup.csv   - Preprocessed highway lookup file");
        System.out.println("  Output.csv   - Destination for processed GPS records");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --help       - Display this help message");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java HiChan GPS.csv Lookup.csv Output.csv");
        System.out.println();
        System.out.println("Unmatched GPS records will be left without a Channel ID.");
        System.out.println("========================================");
    }

    /** Processes one GPS log and writes a copy with Channel ID as the final column. */
    public static void process(Path gpsPath, Path lookupPath, Path outputPath) throws IOException {
        long startTime = System.nanoTime();
        if (!Files.isRegularFile(gpsPath) || !Files.isReadable(gpsPath)) {
            throw new IOException("GPS log file was not found or cannot be read: " + gpsPath);
        }
        if (!Files.isRegularFile(lookupPath) || !Files.isReadable(lookupPath)) {
            throw new IOException("Lookup file was not found or cannot be read: " + lookupPath);
        }

        Path gpsAbsolute = gpsPath.toAbsolutePath().normalize();
        Path lookupAbsolute = lookupPath.toAbsolutePath().normalize();
        Path outputAbsolute = outputPath.toAbsolutePath().normalize();
        if (outputAbsolute.equals(gpsAbsolute) || outputAbsolute.equals(lookupAbsolute)) {
            throw new IOException("Output file must not overwrite either input file.");
        }

        List<String[]> rows = CSVUtils.readCsv(gpsPath.toString());
        if (rows.size() < 2) {
            throw new IOException("GPS log must contain a header and at least one record.");
        }

        String[] header = rows.get(0);
        Map<String, Integer> columns = findGpsColumns(header);
        int latitudeColumn = columns.get("latitude");
        int longitudeColumn = columns.get("longitude");

        List<GPSRecord> records = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            String[] fields = rows.get(i);
            if (fields.length != header.length) {
                throw new IOException("GPS record " + (i + 1)
                        + " has " + fields.length + " fields; expected " + header.length + ".");
            }

            try {
                double latitude = Double.parseDouble(fields[latitudeColumn].trim());
                double longitude = Double.parseDouble(fields[longitudeColumn].trim());
                // GeoHash performs the range and finite-value checks.
                GeoHash.encode(latitude, longitude, GeoHash.PRECISION);
                records.add(new GPSRecord(latitude, longitude, fields));
            } catch (RuntimeException e) {
                throw new IOException("Invalid latitude or longitude in GPS record "
                        + (i + 1) + ": " + e.getMessage(), e);
            }
        }

        GeohashLookup lookup = new GeohashLookup(lookupPath);
        List<HighwaySegment> matchedSegments = new ArrayList<>();
        HighwaySegment previous = null;
        int unmatched = 0;

        for (GPSRecord record : records) {
            HighwaySegment segment = lookup.findBest(
                    record.getLatitude(), record.getLongitude(), previous);
            if (segment != null
                    && "I90".equalsIgnoreCase(segment.getRoute())
                    && ("WA".equals(segment.getState()) || "ID".equals(segment.getState()))) {
                matchedSegments.add(segment);
                previous = segment;
            } else {
                matchedSegments.add(null);
                unmatched++;
            }
        }

        // Use nearby matched records to fill short gaps in the geographic index.
        fillShortGaps(records, matchedSegments);

        unmatched = 0;
        for (HighwaySegment segment : matchedSegments) {
            if (segment == null) {
                unmatched++;
            }
        }

        String direction = new Direction().determine(matchedSegments);
        ChannelIDGenerator generator = new ChannelIDGenerator();
        Map<String, String> knownChannels = new HashMap<>();
        int assigned = 0;

        for (int i = 0; i < records.size(); i++) {
            GPSRecord record = records.get(i);
            HighwaySegment segment = matchedSegments.get(i);
            if (segment == null) {
                continue;
            }

            String coordinateKey = record.getLatitude() + "," + record.getLongitude();
            String channelId = knownChannels.get(coordinateKey);
            if (channelId == null && !direction.isEmpty()) {
                channelId = generator.generate(segment, direction);
            }

            if (channelId != null && !channelId.isEmpty()) {
                record.setChannelId(channelId);
                knownChannels.put(coordinateKey, channelId);
                assigned++;
            }
        }

        // Create the output directory if it does not exist.
        Path outputDirectory = outputPath.toAbsolutePath().getParent();

        if (outputDirectory != null) {
            Files.createDirectories(outputDirectory);
        }

        new GPSWriter().write(outputPath.toString(), header, records);

        System.out.println("Hi_Chan processing complete.");
        System.out.println("GPS records read: " + records.size());
        System.out.println("Channel IDs assigned: " + assigned);
        System.out.println("Unmatched GPS records: " + unmatched);
        System.out.println("Direction: " + (direction.isEmpty() ? "undetermined" : direction));
        if (unmatched > 0) {
            System.out.println("Note: unmatched locations were left without a Channel ID.");
        }
        if (direction.isEmpty()) {
            System.out.println("Note: there were not enough suitable milepost observations to determine direction.");
        }
        System.out.println("Output: " + outputPath.toAbsolutePath().normalize());
        long endTime = System.nanoTime();
        double executionTime = (endTime - startTime) / 1_000_000.0;

        System.out.printf("Execution time: %.2f ms%n", executionTime);
    }


    /**
     * Uses the next/previous matched GPS records to fill gaps of at most two records.
     * A gap is filled only when its surrounding records are in the same state and route.
     */
    private static void fillShortGaps(List<GPSRecord> records,
                                      List<HighwaySegment> segments) {
        final int maxGap = 2;
        int i = 0;

        while (i < segments.size()) {
            if (segments.get(i) != null) {
                i++;
                continue;
            }

            int start = i;
            while (i < segments.size() && segments.get(i) == null) {
                i++;
            }
            int end = i - 1;
            int gapSize = end - start + 1;
            if (gapSize > maxGap) {
                continue;
            }

            HighwaySegment before = start > 0 ? segments.get(start - 1) : null;
            HighwaySegment after = i < segments.size() ? segments.get(i) : null;

            if (before != null && after != null
                    && (!before.getState().equals(after.getState())
                    || !before.getRoute().equals(after.getRoute()))) {
                continue;
            }

            HighwaySegment reference = before != null ? before : after;
            if (reference == null) {
                continue;
            }

            for (int position = start; position <= end; position++) {
                double milepost = reference.getRepresentativeMilepost();

                if (before != null && after != null) {
                    double fraction = (double) (position - start + 1) / (gapSize + 1);
                    milepost = before.getRepresentativeMilepost()
                            + (after.getRepresentativeMilepost()
                            - before.getRepresentativeMilepost()) * fraction;
                }

                GPSRecord record = records.get(position);
                String hash = GeoHash.encode(record.getLatitude(),
                        record.getLongitude(), GeoHash.PRECISION);
                segments.set(position, new HighwaySegment(
                        reference.getRoute(), reference.getStateFips(),
                        reference.getState(), reference.getBeginMilepost(),
                        reference.getEndMilepost(), milepost, hash));
            }
        }
    }

    private static Map<String, Integer> findGpsColumns(String[] header) throws IOException {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            String name = header[i].replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
            columns.put(name, i);
        }
        if (!columns.containsKey("latitude") || !columns.containsKey("longitude")) {
            throw new IOException("GPS CSV must contain Latitude and Longitude columns.");
        }
        return columns;
    }
}
