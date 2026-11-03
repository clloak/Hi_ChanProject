/**
 * represents one row from the input GPS log.
 * the original fields are kept so the output file can preserve the GPS data
 * and append the Channel ID as a new final column.
 */
public class GPSRecord {

    private double latitude;
    private double longitude;
    private String[] originalFields;
    private String channelId;

    public GPSRecord(double latitude, double longitude, String[] originalFields) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.originalFields = originalFields;
        this.channelId = "";
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String[] getOriginalFields() {
        return originalFields;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
}
