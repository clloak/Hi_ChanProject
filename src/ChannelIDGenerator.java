/** Creates a Channel ID from a matched highway location and travel direction. */
public class ChannelIDGenerator {

    public String generate(HighwaySegment segment, String direction) {
        if (segment == null || direction == null
                || !(direction.equals("EB") || direction.equals("WB"))
                || !Double.isFinite(segment.getRepresentativeMilepost())
                || segment.getRepresentativeMilepost() < 0) {
            return "";
        }

        int channelNumber = calculateChannelNumber(segment.getRepresentativeMilepost());
        return "#" + segment.getRoute() + segment.getState()
                + direction + "_" + channelNumber;
    }

    /** Mileposts 0 through 10 use channel 10, 10 through 20 use channel 20, etc. */
    private int calculateChannelNumber(double milepost) {
        return Math.max(10, (int) Math.ceil(milepost / 10.0) * 10);
    }
}
