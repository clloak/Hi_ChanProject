import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Basic CSV reading and writing for the project's GPS and lookup files. */
public class CSVUtils {

    public static List<String[]> readCsv(String filePath) throws IOException {
        List<String[]> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(
                Path.of(filePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    records.add(parseLine(line));
                }
            }
        }
        return records;
    }

    /** Parses one CSV row, including quoted commas and escaped quote characters. */
    public static String[] parseLine(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);

            if (current == '"') {
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

        if (insideQuotes) {
            throw new IOException("CSV row contains an unclosed quoted field.");
        }

        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    public static void writeCsv(String filePath, List<String[]> records) throws IOException {
        Path output = Path.of(filePath);
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            for (String[] record : records) {
                for (int i = 0; i < record.length; i++) {
                    String value = record[i] == null ? "" : record[i];
                    if (value.contains(",") || value.contains("\"")
                            || value.contains("\n") || value.contains("\r")) {
                        value = "\"" + value.replace("\"", "\"\"") + "\"";
                    }
                    writer.write(value);
                    if (i < record.length - 1) {
                        writer.write(",");
                    }
                }
                writer.newLine();
            }
        }
    }
}
