# Stock Analyzer — Run Guide

## Files Required

Make sure you have the following directory structure before proceeding.  
The `out/` folder is generated automatically and does **not** need to be shared.

```
CS5004_Final_Project-main/
├── src/
│   ├── model/
│   │   ├── StockData.java
│   │   ├── EventType.java
│   │   ├── PricePoint.java
│   │   ├── MarketEvent.java
│   │   ├── EarningsEvent.java
│   │   ├── StockDataModel.java
│   │   └── AnalysisResult.java
│   ├── view/
│   │   ├── MainFrame.java
│   │   ├── PriceChartPanel.java
│   │   ├── EventTablePanel.java
│   │   └── AnalysisPanel.java
│   └── controller/
│       ├── CsvLoader.java
│       ├── StockController.java
│       └── ModelDemo.java
├── test/
│   ├── PricePointTest.java
│   ├── MarketEventTest.java
│   ├── EarningsEventTest.java
│   ├── StockDataModelTest.java
│   └── AnalysisResultTest.java
├── data/
│   ├── TSLA_price.csv
│   └── TSLA_news.csv
└── compile.sh
```

---

## Prerequisites

| Requirement | Minimum version |
|---|---|
| Java JDK | 14 or above (project tested on JDK 25) |

Check your version with:

```bash
java -version
javac -version
```

Download JDK if needed: https://www.oracle.com/java/technologies/downloads/

---

## How to Compile and Run

Open a terminal and navigate to the project root folder:

```bash
cd path/to/CS5004_Final_Project-main
```

### Option A — Use the provided script (Mac / Linux)

Make the script executable once:

```bash
chmod +x compile.sh
```

Then run:

```bash
./compile.sh run     # compile and launch the GUI
./compile.sh demo    # compile and run the command-line demo
./compile.sh         # compile only
```

### Option B — Manual commands (Mac / Linux / Windows)

```bash
# 1. Compile model (no dependencies)
javac -d out src/model/*.java

# 2. Compile controller (depends on model)
javac -d out -cp out src/controller/*.java

# 3. Compile view (depends on model and controller)
javac -d out -cp out src/view/*.java

# 4. Launch the GUI
java -cp out view.MainFrame
```

> **Windows note:** replace `:` with `;` in `-cp` arguments, e.g. `-cp out;lib\...`

---

## Using the Application

1. **Launch** — the app auto-loads `data/TSLA_price.csv` and `data/TSLA_news.csv` on startup and runs an initial analysis automatically. No manual steps needed.

2. **Change date range** — edit the From / To fields (format: `YYYY-MM-DD`) and click **Run Analysis** to refresh all panels.

3. **Change anomaly threshold** — adjust the "Anomaly threshold (%)" spinner. Days where the price moved by at least this percentage will be flagged.

4. **Load a different stock** — click **Load CSV…** and select your own price and news CSV files. The ticker is read from the filename automatically (e.g. `AAPL_price.csv` → ticker `AAPL`).

5. **Error messages** — invalid dates or reversed date ranges show a popup dialog and highlight the affected field in red.

---

## CSV Format

If you want to load data for another stock, prepare two CSV files with these columns:

**Price CSV** (e.g. `AAPL_price.csv`):

```
Date,Open,High,Low,Close,Volume,PercentChange
2022-01-03 00:00:00-05:00,182.63,182.94,179.12,182.01,104487900,0.012
...
```

**News CSV** (e.g. `AAPL_news.csv`):

```
date,datetime_unix,headline,summary,source,url,category
2022-01-03,1641211200,Apple hits record market cap,...,Reuters,https://...,company
...
```

The ticker symbol displayed in the app is derived from the price filename prefix before the first underscore.

---

## Running Tests (optional)

Tests require the JUnit 5 standalone console JAR placed in a `lib/` folder:

1. Download `junit-platform-console-standalone-<version>.jar` from https://junit.org/junit5/
2. Place it at `lib/junit-platform-console-standalone.jar`
3. Run:

```bash
./compile.sh test
```
