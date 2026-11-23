# Hi_Chan

## What the program does

Hi_Chan is a small Java command-line application that processes saved GPS logs from trucks traveling on Interstate 90 in Washington and Idaho.

For each GPS record, it uses a prepared geohash lookup file to find a likely I-90 segment. It determines whether the trip is eastbound (EB) or westbound (WB), assigns the segment to a 10-mile channel, and appends a Channel ID to the original GPS record.

Examples:

- `#I90WAEB_10` means eastbound I-90 in Washington, mileposts 0 through 10.
- `#I90IDWB_30` means westbound I-90 in Idaho, mileposts 30 down to 20.

This is a proof-of-concept program. It processes saved GPS CSV files, not live GPS data. It does not include a map or a graphical interface.

## Project files

- `../../Documents/CSCD350-SoftwareDevelopmentPrinciples/Hi_Chan_Project_Completed/Hi_Chan_Project/src` contains the Java source code and unit tests.
- `data/I90_WA_ID.json` contains the NHPN geographic input used by the preprocessor.
- `data/Lookup.csv` is the prepared lookup file used by Hi_Chan.
- `data/20260922-160725 - I90EB.csv` is the supplied eastbound I-90 test log.
- `data/20260922-131715 - I90WB.csv` is the supplied westbound I-90 test log.
- `data/AcceptanceTest_I80EBSacramento.csv` is an out-of-area acceptance test log. It should not receive I-90 Channel IDs.
- `../../Documents/CSCD350-SoftwareDevelopmentPrinciples/Hi_Chan_Project_Completed/Hi_Chan_Project/documentation` contains the project vision, requirements, architecture, and design documents.
- `../../Documents/CSCD350-SoftwareDevelopmentPrinciples/Hi_Chan_Project_Completed/Hi_Chan_Project/testing` contains the integration, system, and acceptance test plans and defect log.

## Requirements

- Java 17 or newer.
- No internet connection or database is required when running the application.
- The GPS input must be a CSV file with `Latitude` and `Longitude` columns.
- The lookup file must use the columns `GEOHASH,ROUTE,STFIPS,STATE,BEGMP,ENDMP,MILEPOST`.

## Compile the program

Open a terminal in the `src` folder and run:

```bash
javac *.java
```

## Prepare the lookup file

The preprocessor is a separate developer tool. It reads the NHPN JSON file and creates the lookup CSV:

```bash
java NHPNPreprocessor ../data/I90_WA_ID.json ../data/Lookup.csv
```

The supplied `Lookup.csv` is already prepared, so this step only needs to be repeated if the JSON data or preprocessing logic changes.

## Run Hi_Chan

A runnable `../../Documents/CSCD350-SoftwareDevelopmentPrinciples/Hi_Chan_Project_Completed/Hi_Chan_Project/HiChan.jar` is included. From the project root, provide the GPS log, lookup file, and output file:

```bash
java -jar HiChan.jar "data/20260922-160725 - I90EB.csv" "data/Lookup.csv" "data/I90EB_Output.csv"
```

You can also run the compiled class from the `src` folder. Provide the GPS log, lookup file, and output file:

```bash
java HiChan "../data/20260922-160725 - I90EB.csv" "../data/Lookup.csv" "../data/I90EB_Output.csv"
```

For the westbound log:

```bash
java HiChan "../data/20260922-131715 - I90WB.csv" "../data/Lookup.csv" "../data/I90WB_Output.csv"
```

The output file contains the original GPS columns in the same order, followed by a `Channel ID` column. The program will not overwrite either input file.

## Run the tests

From the `src` folder:

```bash
java -ea HiChanUnitTests
```

This runs the component tests.

To also regenerate a temporary lookup file from the supplied NHPN JSON and test the complete application with all three supplied GPS logs, run:

```bash
java -ea HiChanUnitTests ../data/I90_WA_ID.json ../data/Lookup-Test.csv ../data
```

The extended tests check eastbound and westbound I-90 processing, the out-of-area I-80 acceptance log, and a simulated short gap in the GPS lookup data. Temporary test output files and the generated test lookup are removed when the tests finish.

## How the program handles records

- Direction is determined from at least three distinct milepost observations within the same state.
- Increasing mileposts indicate eastbound travel; decreasing mileposts indicate westbound travel.
- A Washington/Idaho milepost reset is not treated as a direction reversal.
- Repeated coordinates reuse the Channel ID already assigned to that location.
- A short unmatched gap of up to two GPS records can be filled using surrounding matches when the records belong to the same state and route.
- Longer or unsupported unmatched locations are left without a Channel ID rather than being assigned a guessed I-90 value.
- Original GPS fields and record order are preserved.

## Data and design note

The current implementation and prepared lookup use precision-5 geohashes. 

The supplied `I90_WA_ID.json` has the expected GeoJSON/NHPN-style structure and contains I-90 records for Washington and Idaho. Its exact source release and currency have not been independently verified as part of this code update. If a newer official NHPN extract is required, it should be checked for compatible fields before replacing this file.

## Limitations

This is a classroom proof of concept, not a production trucking service. The geohash lookup and milepost values are approximate. A match identifies a likely I-90 segment, not a surveyed vehicle position. The program is intended for the supplied route and sample data.
