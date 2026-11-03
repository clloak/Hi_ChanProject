/**
 * creates a Channel ID from a matched highway location and travel direction.
 * example: #I90WAEB_10
 */
public class ChannelIDGenerator {

    public String generate(HighwaySegment segment, String direction) {
        if (segment == null) {
            return "";
        }

        // direction will be determined by a separate part of the application.
        if (direction == null || direction.isBlank()) {
            direction = "??";
        }

        int channelNumber = calculateChannelNumber(segment.getMilepost());

        return "#" + segment.getRoute()
                + segment.getState()
                + direction
                + "_" + channelNumber;
    }

    /**
     * places a milepost into a 10-mile channel group
     * for example, mileposts from 0 through 10 use channel 10
     */
    private int calculateChannelNumber(double milepost) {
        int channelNumber = (int) (Math.ceil(milepost / 10.0) * 10);

        // the first channel is numbered 10, including milepost 0.
        if (channelNumber < 10) {
            channelNumber = 10;
        }

        return channelNumber;
    }
}
