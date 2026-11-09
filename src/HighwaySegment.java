/** One highway feature associated with a geohash cell. */
public class HighwaySegment {
    private final String route, stateFips, state, geohash;
    private final double beginMilepost, endMilepost, representativeMilepost;

    public HighwaySegment(String route, String stateFips, String state,
                          double beginMilepost, double endMilepost,
                          double representativeMilepost, String geohash) {
        this.route = route;
        this.stateFips = stateFips;
        this.state = state == null || state.isBlank() ? stateFromFips(stateFips) : state;
        this.beginMilepost = beginMilepost;
        this.endMilepost = endMilepost;
        this.representativeMilepost = representativeMilepost;
        this.geohash = geohash;
    }
    public String getRoute() { return route; }
    public String getStateFips() { return stateFips; }
    public String getState() { return state; }
    public double getBeginMilepost() { return beginMilepost; }
    public double getEndMilepost() { return endMilepost; }
    public double getRepresentativeMilepost() { return representativeMilepost; }
    public String getGeohash() { return geohash; }
    public int getChannelNumber() {
        return Math.max(10, (int) Math.ceil(representativeMilepost / 10.0) * 10);
    }
    public static String stateFromFips(String fips) {
        if (fips == null) return "??";
        String value = fips.trim().replaceFirst("^0+(?!$)", "");
        if ("16".equals(value)) return "ID";
        if ("53".equals(value)) return "WA";
        return "??";
    }
}
