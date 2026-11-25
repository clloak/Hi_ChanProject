# Hi_Chan

Hi_Chan is a Java command-line application that processes saved GPS logs and assigns 10-mile highway Channel IDs for Interstate 90 in Washington and Idaho.

## Requirements

- Java 25 or newer.
- GPS CSV file containing `Latitude` and `Longitude` columns.
- The included `data/Lookup.csv` file.

Tested on Windows 11. No internet connection or additional libraries are required.

## Run Hi_Chan

Download and extract the release ZIP. Open PowerShell in the extracted folder and run:

```powershell
java -jar HiChan.jar "GPS_Log.csv" "data/Lookup.csv" "output/Output.csv"
```

Replace `GPS_Log.csv` with the path to your GPS log. The three arguments are the GPS input file, lookup file, and output file, in that order.

To display help:

```powershell
java -jar HiChan.jar --help
```

## Output

The output CSV preserves the original GPS records and appends a `Channel ID` column. Unresolved records receive a blank Channel ID.

For example, `#I90WAEB_10` represents eastbound I-90 in Washington, in the first 10-mile highway interval.

Hi_Chan is an academic proof of concept. Highway matches and milepost estimates are approximate.

Developed by Team 3 for CSCD 350 at Eastern Washington University.
