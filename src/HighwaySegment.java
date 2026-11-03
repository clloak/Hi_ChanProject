/**
 * represents a highway location found in the prepared lookup file.
 * a lookup entry connects a geohash to a route, state, and milepost.
 */
public class HighwaySegment {

    private String route;
    private String state;
    private double milepost;
    private String geohash;

    public HighwaySegment(String route, String state, double milepost, String geohash) {
        this.route = route;
        this.state = state;
        this.milepost = milepost;
        this.geohash = geohash;
    }

    public String getRoute() {
        return route;
    }

    public String getState() {
        return state;
    }

    public double getMilepost() {
        return milepost;
    }

    public String getGeohash() {
        return geohash;
    }
}
