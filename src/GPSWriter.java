import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Writes original GPS fields followed by the generated Channel ID. */
public class GPSWriter {

    public void write(String outputPath, String[] originalHeader,
                      List<GPSRecord> records) throws IOException {
        String[] outputHeader = new String[originalHeader.length + 1];
        System.arraycopy(originalHeader, 0, outputHeader, 0, originalHeader.length);
        outputHeader[originalHeader.length] = "Channel ID";

        List<String[]> rows = new ArrayList<>();
        rows.add(outputHeader);

        for (GPSRecord record : records) {
            String[] original = record.getOriginalFields();
            String[] output = new String[original.length + 1];
            System.arraycopy(original, 0, output, 0, original.length);
            output[original.length] = record.getChannelId();
            rows.add(output);
        }

        CSVUtils.writeCsv(outputPath, rows);
    }
}
