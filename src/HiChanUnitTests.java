import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Simple dependency-free unit tests for GeoHash and GeohashLookup. */
public class HiChanUnitTests {
    private static int passed = 0;

    public static void main(String[] args) throws Exception {
        testKnownGeohash();
        testInvalidCoordinates();
        testNeighborSearchIncludesCenter();
        testLookupRetainsDuplicateGeohashes();
        testLookupSelectsNearbyPreviousSegment();

        if (args.length == 2) {
            testPreprocessor(args[0], args[1]);
        } else if (args.length != 0) {
            throw new IllegalArgumentException("Usage: java HiChanUnitTests [NHPN.json generatedLookup.csv]");
        }

        System.out.println("All tests passed: " + passed);
    }

    private static void testKnownGeohash() {
        String actual = GeoHash.encode(37.7749, -122.4194, 5);
        check("9q8yy".equals(actual), "Known coordinate encodes to 9q8yy");
    }

    private static void testInvalidCoordinates() {
        boolean thrown = false;
        try { GeoHash.encode(91.0, -117.0, 5); }
        catch (IllegalArgumentException expected) { thrown = true; }
        check(thrown, "Latitude outside valid range is rejected");
    }

    private static void testNeighborSearchIncludesCenter() {
        Set<String> hashes = GeoHash.neighbors(47.6588, -117.4260, 5);
        String center = GeoHash.encode(47.6588, -117.4260, 5);
        check(hashes.contains(center), "Neighbor search includes exact geohash");
        check(hashes.size() >= 1 && hashes.size() <= 9, "Neighbor search returns no more than nine cells");
    }

    private static void testLookupRetainsDuplicateGeohashes() throws IOException {
        String hash = GeoHash.encode(47.6588, -117.4260, 5);
        Path csv = Files.createTempFile("hichan-lookup-test", ".csv");
        try {
            Files.writeString(csv,
                    "GEOHASH,ROUTE,STFIPS,STATE,BEGMP,ENDMP,MILEPOST\n" +
                            hash + ",I90,53,WA,10,20,12\n" +
                            hash + ",I90,16,ID,30,40,32\n");
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
                    "GEOHASH,ROUTE,STFIPS,STATE,BEGMP,ENDMP,MILEPOST\n" +
                            hash + ",I90,53,WA,10,20,12\n" +
                            hash + ",I90,16,ID,30,40,32\n");
            GeohashLookup lookup = new GeohashLookup(csv);
            HighwaySegment previous = new HighwaySegment("I90", "53", "WA", 10, 20, 11, hash);
            HighwaySegment selected = lookup.findBest(47.6588, -117.4260, previous);
            check(selected != null && "WA".equals(selected.getState()),
                    "Lookup prefers same-state candidate near previous milepost");
        } finally {
            Files.deleteIfExists(csv);
        }
    }

    private static void testPreprocessor(String input, String output) throws IOException {
        int rows = NHPNPreprocessor.createLookup(Path.of(input), Path.of(output));
        check(rows > 0, "Preprocessor creates lookup rows from NHPN JSON");
        String header = Files.readAllLines(Path.of(output)).get(0);
        check(header.equals("GEOHASH,ROUTE,STFIPS,STATE,BEGMP,ENDMP,MILEPOST"),
                "Preprocessor writes expected lookup columns");
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError("FAILED: " + description);
        passed++;
        System.out.println("PASS: " + description);
    }
}
