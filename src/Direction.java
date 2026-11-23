import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Determines travel direction from changes in mileposts. */
public class Direction {

    /**
     * Uses three different milepost observations from the same state.
     * A state-line milepost reset is therefore not mistaken for a direction change.
     */
    public String determine(List<HighwaySegment> segments) {
        Map<String, List<Double>> milepostsByState = new LinkedHashMap<>();

        for (HighwaySegment segment : segments) {
            if (segment == null) {
                continue;
            }

            List<Double> mileposts = milepostsByState.computeIfAbsent(
                    segment.getState(), key -> new ArrayList<>());
            double milepost = segment.getRepresentativeMilepost();

            // Several nearby GPS records can fall in the same geohash cell.
            // Count a new observation only when its representative milepost changes.
            if (mileposts.isEmpty()
                    || Math.abs(milepost - mileposts.get(mileposts.size() - 1)) > 0.05) {
                mileposts.add(milepost);
            }

            if (mileposts.size() >= 3) {
                double change = mileposts.get(2) - mileposts.get(0);
                if (change > 0.05) {
                    return "EB";
                }
                if (change < -0.05) {
                    return "WB";
                }
            }
        }

        return "";
    }
}
