# Hi_Chan

CSCD 350 - Software Development Principles | Fall 2026 | Team 3

## What the program does

Hi_Chan is a Java command-line application that processes saved GPS logs from trucks traveling on Interstate 90 in Washington and Idaho. It uses a prepared geohash lookup file to identify likely highway segments, determine travel direction, and append a Channel ID representing a 10-mile highway interval to each GPS record.

Examples:

- `#I90WAEB_10`: Eastbound I-90 in Washington, mileposts 0 through 10.
- `#I90IDWB_30`: Westbound I-90 in Idaho, in the 10-mile interval ending at milepost 30.

The application is a proof of concept. It processes saved GPS CSV files rather than live GPS data and does not include a map or graphical interface.

## Project files

| File or folder | Description |
| --- | --- |
| `src/` | Java source code and automated tests. |
| `data/I90_WA_ID.json` | Instructor-supplied NHPN geographic data used by the preprocessor. |
| `data/Lookup.csv` | Prepared precision-5 geohash lookup containing 1,468 data records. |
| `data/20260922-160725 - I90EB.csv` | Eastbound I-90 GPS log. |
| `data/20260922-131715 - I90WB.csv` | Westbound I-90 GPS log. |
| `data/AcceptanceTest_I80EBSacramento.csv` | Out-of-area acceptance test log. |
| `documentation/` | Project vision, requirements, architecture, detailed design, and traceability documents. |
| `testing/` | Test plans, test reports, and defect reporting workbook. |
| `HiChan.jar` | Runnable application JAR. |

### Java source files

| File | Responsibility |
| --- | --- |
| `HiChan.java` | Command-line entry point and GPS processing workflow. |
| `NHPNPreprocessor.java` | Generates the lookup CSV from the NHPN JSON file. |
| `JSONParser.java` | Parses JSON data. |
| `CSVUtils.java` | Reads, parses, and formats CSV data. |
| `GeoHash.java` | Encodes coordinates and identifies neighboring geohash cells. |
| `GeohashLookup.java` | Loads the lookup index and selects highway candidates. |
| `HighwaySegment.java` | Stores highway, state, geohash, and milepost information. |
| `GPSRecord.java` | Stores GPS record data and its Channel ID. |
| `Direction.java` | Determines eastbound or westbound travel. |
| `ChannelIDGenerator.java` | Generates formatted Channel IDs. |
| `GPSWriter.java` | Writes GPS records with appended Channel IDs. |
| `HiChanUnitTests.java` | Contains the automated test suite. |

There are 11 production Java source files and one separate test source file.

## Requirements

- Windows 11.
- OpenJDK 25.0.4.1 LTS (course-specified version).
- IntelliJ IDEA for development, or PowerShell with Java available on the PATH.
- No external Java libraries, database, or internet connection is required for normal application processing.

The GPS input must be a CSV file containing `Latitude` and `Longitude` columns and at least one data record. The prepared lookup CSV must use the following columns:

```text
GEOHASH,ROUTE,STFIPS,STATE,BEGMP,ENDMP,MILEPOST
```

## Compile the program

Open the project in IntelliJ IDEA and select the course-specified JDK. Open its terminal from the project root and run:

```powershell
cd src
javac *.java
```

The following source-code commands assume that the terminal is in the `src` folder.

## Prepare the lookup file

The preprocessor is a separate developer utility. It reads the instructor-supplied NHPN geographic data, estimates mileposts along highway geometry, and writes a precision-5 geohash lookup CSV.

```powershell
java NHPNPreprocessor ../data/I90_WA_ID.json ../data/Lookup.csv
```

The supplied `Lookup.csv` is already prepared and is included with the project. Preprocessing is not required during normal application execution. Repeat this step only if the geographic input or preprocessing logic changes.

## Run Hi_Chan

The application requires three command-line arguments in this order:

```text
GPS input CSV, lookup CSV, output CSV
```

From the project root, run the executable JAR (see the rebuild instructions below):

```powershell
java -jar HiChan.jar "data/20260922-160725 - I90EB.csv" "data/Lookup.csv" "output/I90EB_Output.csv"
```

Alternatively, run the compiled application directly from the `src` folder.

Eastbound I-90:

```powershell
java HiChan "../data/20260922-160725 - I90EB.csv" "../data/Lookup.csv" "../output/I90EB_Output.csv"
```

Westbound I-90:

```powershell
java HiChan "../data/20260922-131715 - I90WB.csv" "../data/Lookup.csv" "../output/I90WB_Output.csv"
```

Out-of-area I-80 acceptance test:

```powershell
java HiChan "../data/AcceptanceTest_I80EBSacramento.csv" "../data/Lookup.csv" "../output/I80_Output.csv"
```

Display command-line help:

```powershell
java HiChan --help
```

The final application creates missing output directories when possible, preserves the original GPS fields and record order, and appends `Channel ID` as the last column. The designated output path must not overwrite either input file. An existing separate output file may be replaced.

After processing, Hi_Chan displays the number of GPS records read, Channel IDs assigned, unmatched GPS records, detected direction, output location, and execution time. Missing or invalid required files produce error messages instead of a successful completion message.

## Rebuild the executable JAR

After making source-code changes, rebuild `HiChan.jar` so that it contains the current implementation. From the `src` folder, compile into a clean build directory and package the compiled classes:

```powershell
New-Item -ItemType Directory -Path ../build -Force | Out-Null
javac -d ../build *.java
jar --create --file ../HiChan.jar --main-class HiChan -C ../build .
```

Use an empty build directory to avoid packaging stale class files. Return to the project root and check the executable:

```powershell
cd ..
java -jar HiChan.jar --help
```

## Run the tests

From the `src` folder, compile the source files and run the basic component tests:

```powershell
javac *.java
java -ea HiChanUnitTests
```

The basic suite contains 13 checks. To run all 19 checks, including preprocessing and full GPS processing, use:

```powershell
java -ea HiChanUnitTests ../data/I90_WA_ID.json ../data/Lookup-Test.csv ../data
```

The full suite checks coordinate validation, geohash generation and lookup, direction detection, channel boundaries, CSV processing, preprocessing, eastbound and westbound GPS processing, the out-of-area acceptance log, and short-gap handling. The temporary test lookup and output files are removed after a successful extended run. Use a separate temporary lookup filename to avoid replacing the supplied `Lookup.csv`.

### Recorded test results

All 19 automated checks passed during the team's recorded Windows 11 test run.

| Test log | Records read | Channel IDs assigned |
| --- | ---: | ---: |
| I-90 Eastbound | 128 | 128 |
| I-90 Westbound | 124 | 124 |
| I-80 Acceptance | 128 | 0 |

The I-80 acceptance log is preserved with blank Channel IDs because it is outside the supported I-90 area. Refer to the test reports for individual test scenarios and their verification status.

## How the program handles records

- GPS coordinates are encoded with precision-5 geohashes. Exact and neighboring cells are searched for highway candidates.
- Multiple highway features can share a geohash. Available state and milepost information helps select a suitable match.
- Direction is determined from at least three suitable milepost observations within the same state. Increasing mileposts indicate eastbound travel, and decreasing mileposts indicate westbound travel.
- A milepost reset at the Washington-Idaho state boundary is not treated as a direction reversal.
- Channel numbers are based on the representative milepost, rounded up to the next 10-mile boundary with a minimum of 10. Milepost 10 belongs to Channel 10; a milepost greater than 10 and no greater than 20 belongs to Channel 20.
- Repeated matched GPS coordinates reuse an already assigned Channel ID when available.
- Unmatched gaps of up to two records can use adjacent highway matches. When matches exist on both sides, they must share the same route and state, and missing mileposts are estimated between them. A short gap at the beginning or end of the log can use its single adjacent match.
- Unresolved matches and insufficient direction information result in blank Channel IDs rather than unsupported assignments.

## Limitations

Hi_Chan is intended for the supplied I-90 route and sample GPS logs in Washington and Idaho. Geohash matching and estimated mileposts are approximate; a match identifies a likely highway segment, not an exact vehicle position. The application is not a production tracking or navigation system.
