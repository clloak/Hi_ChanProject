# Hi_Chan

## Project Description

Hi_Chan is a Java application that processes GPS log files from vehicles traveling along Interstate 90 (I-90) in Washington and Idaho.

The program uses GPS coordinates and geohash lookups to identify highway locations and assign Channel IDs to GPS records.

The final output is a CSV file containing the original GPS information with an additional Channel ID column.

## Technologies

- Java
- IntelliJ IDEA
- Git and GitHub
- CSV files
- JSON data
- Geohashing

## Running the Program

The planned command-line format is:

```bash
java HiChan GPS_Log.csv Lookup.csv Output.csv
```

The NHPN preprocessor will use a separate command:

```bash
java NHPNPreprocessor NHPN.json Lookup.csv
```

## Current Development Status

This project is currently in its initial development stage.

The Java project structure has been created, and the first classes are being developed. Additional functionality will be implemented and tested as development progresses.

## Development Environment

The project is being developed using IntelliJ IDEA and Java.

GitHub is used for version control and collaboration between development devices.

---

**Note:** Some features described in this README are planned functionality and may not be implemented yet.