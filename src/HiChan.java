import java.nio.file.Files;
import java.nio.file.Path;
/**
 * main starting point for the Hi_Chan application.
 * expected arguments:
 * 1. GPS log CSV file
 * 2. Prepared highway lookup CSV file
 * 3. Output CSV file path
 */
// TODO: WIP
public class HiChan {

    public static void main(String[] args) {
        // the application expects exactly three command line arguments
        if (args.length != 3) {
            System.out.println("Usage: java HiChan <GPS_Log.csv> <Lookup.csv> <Output.csv>");
            return;
        }

        String gpsFile = args[0];
        String lookupFile = args[1];
        String outputFile = args[2];

        // check that the two input files exist before processing begins
        if (!Files.isRegularFile(Path.of(gpsFile))) {
            System.out.println("Error: GPS log file was not found: " + gpsFile);
            return;
        }

        if (!Files.isRegularFile(Path.of(lookupFile))) {
            System.out.println("Error: Lookup file was not found: " + lookupFile);
            return;
        }

        System.out.println("Hi_Chan received its input files.");
        System.out.println("GPS log: " + gpsFile);
        System.out.println("Lookup file: " + lookupFile);
        System.out.println("Output file: " + outputFile);

        // TODO: Load the lookup file.
        // TODO: Read and process the GPS records.
        // TODO: Determine direction and generate Channel IDs.
        // TODO: Write the completed records to the output file.

        System.out.println("GPS processing has not been implemented yet.");
    }
}
