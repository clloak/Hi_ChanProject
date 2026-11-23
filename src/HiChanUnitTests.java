import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Simple dependency-free tests for the main Hi_Chan components. */
public class HiChanUnitTests {
    private static int passed = 0;

    public static void main(String[] args) throws Exception {
        testKnownGeohash();
        testInvalidCoordinates();
        testNeighborSearchIncludesCenter();
        testLookupRetainsDuplicateGeohashes();
        testLookupSelectsNearbyPreviousSegment();
        testDirectionEastbound();
        testDirectionWestboundAcrossStateBoundary();
        testChannelIdBoundaries();
        testInvalidDirectionRejected();
        testCsvQuotedFields();
        testGpsWriterPreservesFields();

        if (args.length == 2 || args.length == 3) {
            testPreprocessor(args[0], args[1]);
            if (args.length == 3) {
                try {
                    testFullProcessing(args[1], args[2]);
                } finally {
                    Files.deleteIfExists(Path.of(args[1]));
                }
            }
        } else if (args.length != 0) {
            throw new IllegalArgumentException(
                    "Usage: java -ea HiChanUnitTests [NHPN.json generatedLookup.csv [GPSDataDirectory]]");
        }

        System.out.println("All tests passed: " + passed);
    }

    private static void testKnownGeohash() {
        String actual = GeoHash.encode(37.7749, -122.4194, 5);
        check("9q8yy".equals(actual), "Known coordinate encodes to 9q8yy");
    }

    private static void testInvalidCoordinates() {
        boolean thrown = false;
        try {
            GeoHash.encode(91.0, -117.0, 5);
        } catch (IllegalArgumentException expected) {
            thrown = true;
        }
        check(thrown, "Latitude outside valid range is rejected");
    }

    private static void testNeighborSearchIncludesCenter() {
        Set<String> hashes = GeoHash.neighbors(47.6588, -117.4260, 5);
        String center = GeoHash.encode(47.6588, -117.4260, 5);
        check(hashes.contains(center), "Neighbor search includes exact geohash");
        check(hashes.size() >= 1 && hashes.size() <= 9,
                "Neighbor search returns no more than nine cells");
    }

    private static void testLookupRetainsDuplicateGeohashes() throws IOException {
        String hash = GeoHash.encode(47.6588, -117.4260, 5);
        Path csv = Files.createTempFile("hichan-lookup-test", ".csv");
        try {
            Files.writeString(csv,
                    "GEOHASH,ROUTE,STFIPS,STATE,BEGMP,ENDMP,MILEPOST\n"
                    + hash + ",I90,53,WA,10,20,12\n"
                    + hash + ",I90,16,ID,30,40,32\n");
            GeohashLookup lookup = new GeohashLookup(csv);
            List<HighwaySegment> matches = lookup.findCandidates(47.6588, -117.4260);
            check(matches.size() == 2, "Lookup retains two features sharing one geohash");
        } finally {
            Files.deleteIfExists(csv);
        }
    }

    private static void testLookupSelectsNearbyPreviousSegment() throws IOException {
        String hash = GeoHash.encode(47.6588, -117.4260, 5);
        Path csv = Files.createTempFile("hichan-lookup-select-test", ".csv");
        try {
            Files.writeString(csv,
                    "GEOHASH,ROUTE,STFIPS,STATE,BEGMP,ENDMP,MILEPOST\n"
                    + hash + ",I90,53,WA,10,20,12\n"
                    + hash + ",I90,16,ID,30,40,32\n");
            GeohashLookup lookup = new GeohashLookup(csv);
            HighwaySegment previous =
                    new HighwaySegment("I90", "53", "WA", 10, 20, 11, hash);
            HighwaySegment selected =
                    lookup.findBest(47.6588, -117.4260, previous);
            check(selected != null && "WA".equals(selected.getState()),
                    "Lookup prefers same-state candidate near previous milepost");
        } finally {
            Files.deleteIfExists(csv);
        }
    }

    private static void testDirectionEastbound() {
        List<HighwaySegment> segments = new ArrayList<>();
        segments.add(segment("WA", 280.0));
        segments.add(segment("WA", 280.0));
        segments.add(segment("WA", 282.0));
        segments.add(segment("WA", 284.0));
        check("EB".equals(new Direction().determine(segments)),
                "Direction identifies increasing mileposts as eastbound");
    }

    private static void testDirectionWestboundAcrossStateBoundary() {
        List<HighwaySegment> segments = new ArrayList<>();
        segments.add(segment("ID", 6.0));
        segments.add(segment("ID", 4.0));
        segments.add(segment("ID", 2.0));
        segments.add(segment("WA", 298.0));
        segments.add(segment("WA", 296.0));
        check("WB".equals(new Direction().determine(segments)),
                "Direction identifies westbound travel without treating a state reset as reversal");
    }

    private static HighwaySegment segment(String state, double milepost) {
        String fips = "ID".equals(state) ? "16" : "53";
        return new HighwaySegment("I90", fips, state, milepost, milepost,
                milepost, "c2kz3");
    }

    private static void testChannelIdBoundaries() {
        ChannelIDGenerator generator = new ChannelIDGenerator();
        check("#I90WAEB_10".equals(generator.generate(segment("WA", 10), "EB")),
                "Milepost 10 is assigned to channel 10");
        check("#I90WAEB_20".equals(generator.generate(segment("WA", 10.01), "EB")),
                "Milepost above 10 is assigned to channel 20");
    }

    private static void testInvalidDirectionRejected() {
        String result = new ChannelIDGenerator().generate(segment("WA", 15), "??");
        check(result.isEmpty(), "Invalid direction does not produce a Channel ID");
    }

    private static void testCsvQuotedFields() throws IOException {
        String[] fields = CSVUtils.parseLine("one,\"two, with comma\",\"say \"\"hi\"\"\"");
        check(fields.length == 3 && "two, with comma".equals(fields[1])
                        && "say \"hi\"".equals(fields[2]),
                "CSV parser handles quoted commas and escaped quotes");
    }

    private static void testGpsWriterPreservesFields() throws IOException {
        Path output = Files.createTempFile("hichan-output-test", ".csv");
        try {
            GPSRecord record = new GPSRecord(47.0, -117.0,
                    new String[]{"date", "47.0", "-117.0", "10"});
            record.setChannelId("#I90WAEB_10");
            new GPSWriter().write(output.toString(),
                    new String[]{"date time", "latitude", "longitude", "altitude(m)"},
                    List.of(record));
            List<String[]> rows = CSVUtils.readCsv(output.toString());
            check(rows.size() == 2 && rows.get(1).length == 5
                            && "date".equals(rows.get(1)[0])
                            && "#I90WAEB_10".equals(rows.get(1)[4]),
                    "GPS writer preserves original fields and appends Channel ID");
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private static void testPreprocessor(String input, String output) throws IOException {
        int rows = NHPNPreprocessor.createLookup(Path.of(input), Path.of(output));
        check(rows > 0, "Preprocessor creates lookup rows from NHPN JSON");
        String header = Files.readAllLines(Path.of(output)).get(0);
        check(header.equals("GEOHASH,ROUTE,STFIPS,STATE,BEGMP,ENDMP,MILEPOST"),
                "Preprocessor writes expected lookup columns");
    }

    private static void testFullProcessing(String lookup, String dataDirectory) throws IOException {
        Path data = Path.of(dataDirectory);
        Path ebOut = Files.createTempFile("hichan-eb-test", ".csv");
        Path wbOut = Files.createTempFile("hichan-wb-test", ".csv");
        Path otherOut = Files.createTempFile("hichan-other-test", ".csv");
        try {
            HiChan.process(data.resolve("20260922-160725 - I90EB.csv"),
                    Path.of(lookup), ebOut);
            HiChan.process(data.resolve("20260922-131715 - I90WB.csv"),
                    Path.of(lookup), wbOut);
            HiChan.process(data.resolve("AcceptanceTest_I80EBSacramento.csv"),
                    Path.of(lookup), otherOut);

            List<String[]> eb = CSVUtils.readCsv(ebOut.toString());
            List<String[]> wb = CSVUtils.readCsv(wbOut.toString());
            List<String[]> other = CSVUtils.readCsv(otherOut.toString());
            check(eb.size() == 129 && eb.get(1)[6].startsWith("#I90WAEB_"),
                    "Eastbound I-90 log receives eastbound Channel IDs");
            check(wb.size() == 125 && wb.get(1)[6].startsWith("#I90IDWB_"),
                    "Westbound I-90 log receives westbound Channel IDs");
            boolean noUnsupportedIds = true;
            for (int i = 1; i < other.size(); i++) {
                if (!other.get(i)[6].isEmpty()) {
                    noUnsupportedIds = false;
                    break;
                }
            }
            check(other.size() == 129 && noUnsupportedIds,
                    "Non-I-90 acceptance log does not receive I-90 Channel IDs");

            testShortGapBackfill(data, Path.of(lookup));
        } finally {
            Files.deleteIfExists(ebOut);
            Files.deleteIfExists(wbOut);
            Files.deleteIfExists(otherOut);
        }
    }


    private static void testShortGapBackfill(Path data, Path lookup) throws IOException {
        List<String[]> source = CSVUtils.readCsv(
                data.resolve("20260922-160725 - I90EB.csv").toString());
        String previousHash = "";
        String[] firstMatch = null;
        String[] laterMatch = null;
        int distinct = 0;

        for (int i = 1; i < source.size(); i++) {
            String[] row = source.get(i);
            String hash = GeoHash.encode(Double.parseDouble(row[1]),
                    Double.parseDouble(row[2]), GeoHash.PRECISION);
            if (!hash.equals(previousHash)) {
                distinct++;
                if (distinct == 1) {
                    firstMatch = row;
                } else if (distinct == 3) {
                    laterMatch = row;
                    break;
                }
                previousHash = hash;
            }
        }

        String[] gap = firstMatch.clone();
        gap[1] = "0.0";
        gap[2] = "0.0";

        Path input = Files.createTempFile("hichan-gap-input", ".csv");
        Path output = Files.createTempFile("hichan-gap-output", ".csv");
        try {
            List<String[]> testRows = new ArrayList<>();
            testRows.add(source.get(0));
            testRows.add(firstMatch);
            testRows.add(gap);
            testRows.add(laterMatch);
            CSVUtils.writeCsv(input.toString(), testRows);
            HiChan.process(input, lookup, output);
            List<String[]> result = CSVUtils.readCsv(output.toString());
            check(result.size() == 4 && !result.get(2)[6].isEmpty(),
                    "A short unmatched GPS gap is backfilled from surrounding matches");
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError("FAILED: " + description);
        }
        passed++;
        System.out.println("PASS: " + description);
    }
}
