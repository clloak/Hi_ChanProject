import java.io.*;
import java.util.ArrayList;
import java.util.List;
/**
 * read and process CSV files
 */
// TODO
public class CSVUtils {

    // reads a CSV file and returns all of its rows
    public static List<String[]> readCsv(String filePath) throws IOException {

        List<String[]> records = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;

        while ((line = reader.readLine()) != null) {
            records.add(parseLine(line));
        }
        reader.close();
        return records;
    }

    // separates one CSV line into individual fields
    public static String[] parseLine(String line) {

        List<String> fields = new ArrayList<>();

        StringBuilder field = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                // Handles escaped quotes inside quoted fields.
                if (insideQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;

                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (current == ',' && !insideQuotes) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        // add the final field
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    // writes rows of data into a CSV file
    public static void writeCsv(String filePath, List<String[]> records) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        for (String[] record : records) {

            for (int i = 0; i < record.length; i++) {
                String value = record[i];
                // escape quotes and wrap fields containing commas or quotes
                if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                    value = "\"" + value.replace("\"", "\"\"") + "\"";
                }
                writer.write(value);
                if (i < record.length - 1) {
                    writer.write(",");
                }
            }
            writer.newLine();
        }
        writer.close();
    }
}

