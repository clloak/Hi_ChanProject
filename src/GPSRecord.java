/** One GPS CSV record, retaining the original fields for the output file. */
public class GPSRecord {
    private final double latitude;
    private final double longitude;
    private final String[] originalFields;
    private String channelId = "";

    public GPSRecord(double latitude, double longitude, String[] originalFields) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.originalFields = originalFields.clone();
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String[] getOriginalFields() { return originalFields.clone(); }
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) {
        this.channelId = channelId == null ? "" : channelId;
    }
}
