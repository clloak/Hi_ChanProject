/**
 * Creates a Channel ID from a matched highway location and travel direction.
 * Example: #I90WAEB_10
 */
public class ChannelIDGenerator {

    /**
     * Generates a Channel ID.
     *
     * @param segment matched highway segment
     * @param direction travel direction ("EB" or "WB")
     * @return generated Channel ID
     */
    public String generate(HighwaySegment segment, String direction) {

        if (segment == null) {
            return "";
        }

        if (direction == null || direction.isBlank()) {
            direction = "??";
        }

        int channelNumber = calculateChannelNumber(
                segment.getRepresentativeMilepost());

        return "#" + segment.getRoute()
                + segment.getState()
                + direction
                + "_" + channelNumber;
    }

    /**
     * Places a milepost into a 10-mile channel group.
     * Mileposts 0 through 10 use channel 10.
     * Mileposts greater than 10 through 20 use channel 20.
     */
    private int calculateChannelNumber(double milepost) {

        int channelNumber = (int) Math.ceil(milepost / 10.0) * 10;

        // The first channel is numbered 10, including milepost 0.
        if (channelNumber < 10) {
            channelNumber = 10;
        }

        return channelNumber;
    }
}