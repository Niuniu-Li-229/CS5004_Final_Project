# TSLA Stock Anomaly Analyzer

A Java desktop application that loads historical Tesla (TSLA) stock price and news data, detects
anomalous trading days, and correlates price movements with nearby market events — all presented
through an interactive Swing GUI.

*CS 5004 Final Project · Northeastern University · Spring 2026*

---

## Table of Contents

1. [Goals and Rationale](#goals-and-rationale)
2. [Demo](#demo)
3. [Methods and Tools](#methods-and-tools)
4. [Object-Oriented Design Highlights](#object-oriented-design-highlights)
5. [Project Structure](#project-structure)
6. [Architecture](#architecture)
7. [Prerequisites](#prerequisites)
8. [How to Build and Run](#how-to-build-and-run)
9. [Using the Application](#using-the-application)
10. [CSV Format](#csv-format)
11. [Running the Tests](#running-the-tests)
12. [Limitations and Future Extensions](#limitations-and-future-extensions)
13. [Citations and Resources](#citations-and-resources)
14. [Team](#team)

---

## Goals and Rationale

Stock prices do not move in a vacuum — large single-day swings almost always coincide with
earnings releases, analyst upgrades or downgrades, regulatory actions, or macro-economic news.
The goal of this project is to make that connection visible and interactive.

Given a date range and a user-defined threshold, the application:
- Flags every trading day where the absolute percent change meets or exceeds the threshold
- Searches a ±3-day window around each anomaly for related news events
- Displays results in a price chart, an event table, and a plain-text analysis summary

The project was chosen because it exercises the full MVC pattern, a meaningful class hierarchy,
and real-world data processing — all core themes of CS 5004.

---

## Demo

On launch, the app auto-loads `data/TSLA_price.csv` and `data/TSLA_news.csv` and runs an
initial analysis automatically.

| Panel | Purpose |
|---|---|
| **Price Chart** | Candlestick-style chart with anomaly days highlighted |
| **Event Table** | Scrollable table of news events filtered by the selected date range |
| **Analysis Panel** | Summary text: total return, anomaly count, and per-day explanations |

---

## Methods and Tools

| Category | Choice |
|---|---|
| Language | Java 14+ (tested on JDK 25) |
| GUI framework | Java Swing (`javax.swing`) |
| Data format | CSV (price data + news/event data) |
| Testing framework | JUnit 5 (console standalone launcher) |
| Build | `compile.sh` shell script (no external build tool required) |
| Version control | Git / GitHub |

---

## Object-Oriented Design Highlights

### 1. Inheritance and Abstraction

`StockData` is an abstract base class that unifies all time-stamped data objects under one type.
`PricePoint` and `MarketEvent` both extend it, inheriting date storage and the shared validation
helpers (`requireNonNull`, `requireNonBlank`, `requirePositive`). `EarningsEvent` further extends
`MarketEvent` with earnings-specific fields.

```
StockData  (abstract)
├── PricePoint
└── MarketEvent
    └── EarningsEvent
```

### 2. Polymorphism

Every subclass overrides `getSummary()` declared abstract in `StockData`. The controller and view
can call `getSummary()` on any `StockData` reference and receive the correct output without
knowing the concrete type — a textbook example of runtime polymorphism.

```java
MarketEvent ref = new EarningsEvent(...);
ref.getSummary();  // calls EarningsEvent's override, not MarketEvent's
```

### 3. Encapsulation and Defensive Copies

All fields are `private final`. Any method that returns a collection returns an
`Collections.unmodifiableList(new ArrayList<>(original))` — callers cannot mutate internal state.
Input validation is enforced in every constructor so objects are always in a valid state.

### 4. MVC Architecture

| Layer | Classes | Responsibility |
|---|---|---|
| **Model** | `StockData`, `PricePoint`, `MarketEvent`, `EarningsEvent`, `StockDataModel`, `AnalysisResult` | Data, validation, business rules |
| **Controller** | `StockController`, `CsvLoader` | Orchestration, CSV parsing, anomaly detection |
| **View** | `MainFrame`, `PriceChartPanel`, `EventTablePanel`, `AnalysisPanel` | Swing GUI, user interaction |

The view never touches the model directly — all requests go through `StockController`.

---

## Project Structure

```
CS5004_Final_Project/
├── src/
│   ├── model/
│   │   ├── StockData.java          # Abstract base class
│   │   ├── EventType.java          # Enum: EARNINGS, ANALYST, REGULATORY, MACRO, PRODUCT, OTHER
│   │   ├── PricePoint.java         # One trading day's OHLCV data
│   │   ├── MarketEvent.java        # One news/market event
│   │   ├── EarningsEvent.java      # Earnings-specific subclass of MarketEvent
│   │   ├── StockDataModel.java     # Aggregates prices + events for one ticker
│   │   └── AnalysisResult.java     # Analysis output, including AnomalyPoint inner class
│   ├── controller/
│   │   ├── CsvLoader.java          # Parses price and news CSV files
│   │   ├── StockController.java    # Anomaly detection, event correlation
│   │   └── ModelDemo.java          # CLI demo entry point
│   └── view/
│       ├── MainFrame.java          # Top-level Swing window
│       ├── PriceChartPanel.java    # Price chart with anomaly markers
│       ├── EventTablePanel.java    # Sortable news event table
│       └── AnalysisPanel.java      # Text summary panel
├── test/
│   ├── PricePointTest.java         # 17 tests
│   ├── MarketEventTest.java        # 11 tests
│   ├── EarningsEventTest.java      # 5 tests
│   ├── StockDataModelTest.java     # 17 tests
│   ├── AnalysisResultTest.java     # 21 tests
│   └── StockControllerTest.java    # 10 tests
├── data/
│   ├── TSLA_price.csv
│   └── TSLA_news.csv
├── lib/
│   └── junit-platform-console-standalone.jar   # required for tests
├── out/                            # compiled .class files (auto-generated)
├── compile.sh                      # build and run script
└── README.md
```

---

## Architecture

This project follows the **Model-View-Controller (MVC)** pattern, keeping data, logic, and
presentation in strictly separate layers.

```
  ┌─────────────────────┐     queries / builds      ┌──────────────────────┐    pushes result     ┌─────────────────────┐
  │        MODEL        │ ◄────────────────────────► │     CONTROLLER       │ ──────────────────► │        VIEW         │
  │                     │                            │                      │ ◄─────────────────── │                     │
  │  StockData          │                            │  CsvLoader           │   user actions       │  MainFrame          │
  │  ├─ PricePoint      │                            │  StockController     │   (date, threshold)  │  PriceChartPanel    │
  │  └─ MarketEvent     │                            │                      │                      │  EventTablePanel    │
  │     └─ EarningsEvent│                            │                      │                      │  AnalysisPanel      │
  │  StockDataModel     │                            │                      │                      │                     │
  │  AnalysisResult     │                            │                      │                      │          ▲          │
  │  EventType          │                            │                      │                      │          │          │
  └─────────────────────┘                            └──────────────────────┘                      │        User         │
                                                                                                   └─────────────────────┘
```

| MVC Role | Responsibility |
|---|---|
| **Model** | Holds all data and business rules; never imports Swing; fully unit-tested |
| **Controller** | Loads CSV data, runs anomaly detection, builds `AnalysisResult`; the only layer that touches both Model and View |
| **View** | Renders results and captures user input; contains no business logic |

`StockController.analyze()` scans the loaded price data, computes per-day percent change using
the previous row's close, and flags any day whose absolute move meets the threshold. It then
searches all events within a ±3-day window around each anomaly and builds an `AnalysisResult`
containing the anomaly list, total return, and a plain-text summary.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 14 or above (tested on JDK 25) |

Check your version:

```bash
java -version
javac -version
```

Download JDK if needed: https://www.oracle.com/java/technologies/downloads/

---

## How to Build and Run

Open a terminal and navigate to the project root:

```bash
cd path/to/CS5004_Final_Project
```

### Option A — Shell script (Mac / Linux)

Make the script executable once:

```bash
chmod +x compile.sh
```

Then use one of:

```bash
./compile.sh        # compile only
./compile.sh run    # compile and launch the GUI
./compile.sh demo   # compile and run the command-line demo
./compile.sh test   # compile and run all JUnit tests (requires lib/ — see below)
```

### Option B — Manual commands (Mac / Linux / Windows)

```bash
# 1. Compile model
javac -d out src/model/*.java

# 2. Compile controller
javac -d out -cp out src/controller/*.java

# 3. Compile view
javac -d out -cp out src/view/*.java

# 4. Launch GUI
java -cp out view.MainFrame
```

> **Windows:** replace `:` with `;` in all `-cp` arguments, e.g. `-cp out;lib\...`

---

## Using the Application

1. **Launch** — the app auto-loads `data/TSLA_price.csv` and `data/TSLA_news.csv` on startup
   and runs an initial analysis automatically. No manual steps needed.

2. **Change date range** — edit the From / To fields (format: `YYYY-MM-DD`) and click
   **Run Analysis** to refresh all panels.

3. **Change anomaly threshold** — adjust the "Anomaly threshold (%)" spinner. Days where the
   price moved by at least this percentage will be flagged.

4. **Load a different stock** — click **Load CSV…** and select your own price and news CSV
   files. The ticker is read from the filename automatically
   (e.g. `AAPL_price.csv` → ticker `AAPL`).

5. **Error messages** — invalid dates or reversed date ranges show a popup dialog and highlight
   the affected field in red.

---

## CSV Format

To load data for another stock, prepare two CSV files with these columns:

**Price CSV** (e.g. `AAPL_price.csv`):

```
Date,Open,High,Low,Close,Volume,PercentChange
2022-01-03 00:00:00-05:00,182.63,182.94,179.12,182.01,104487900,0.012
```

**News CSV** (e.g. `AAPL_news.csv`):

```
date,datetime_unix,headline,summary,source,url,category
2022-01-03,1641211200,Apple hits record market cap,...,Reuters,https://...,company
```

The ticker symbol is derived from the price filename prefix before the first underscore.

---

## Running the Tests

**76 tests across 6 test classes — all passing.**

### Setup (one time)

```bash
mkdir lib
curl -Lo lib/junit-platform-console-standalone.jar \
  "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"
```

### Run

```bash
./compile.sh test
```

### Test coverage summary

| Test Class | Tests | What is covered |
|---|---|---|
| `PricePointTest` | 12 | Constructor validation, `getPreviousClose`, derived calculations, `getSummary`, `toString` |
| `MarketEventTest` | 11 | Constructor validation, `isType`, `getSummary`, `toString` |
| `EarningsEventTest` | 5 | Constructor, polymorphic `getSummary` override |
| `StockDataModelTest` | 17 | Constructor, defensive copy, `getPricesInRange`, `getEventsByType` |
| `AnalysisResultTest` | 21 | Constructor, `AnomalyPoint` inner class, unmodifiable lists |
| `StockControllerTest` | 10 | Pre-load state, guard clauses, empty-range analysis |
| **Total** | **76** | |

---

## Limitations and Future Extensions

### Current Limitations

- Data is limited to TSLA
- Event classification uses simple keyword matching and may misclassify ambiguous headlines
- The anomaly detector compares consecutive CSV rows, so gaps (weekends, holidays already
  removed) do not affect accuracy, but missing rows in custom CSVs could produce incorrect
  percent-change values
- No persistent user settings — threshold and date range reset on each launch

### Future Extensions

- Support multi-ticker comparison in a single view
- Replace keyword-based event classification with an NLP model
- Add export functionality (PNG chart, PDF report)
- Integrate a live data feed (e.g. Finnhub WebSocket API) for real-time analysis
- Add a configurable event window size (currently fixed at ±3 days)

---

## Citations and Resources

- Oracle Java SE Documentation — Swing and core Java APIs: https://docs.oracle.com/en/java/javase/
- JUnit 5 User Guide: https://junit.org/junit5/docs/current/user-guide/
- Bloch, J. (2018). *Effective Java* (3rd ed.). Addison-Wesley. — defensive copies, item design
- Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). *Design Patterns*. Addison-Wesley. — MVC, Strategy pattern
- Yahoo Finance. (n.d.). Tesla, Inc. (TSLA) stock price, news, quote & history. Yahoo Finance. Retrieved April 20, 2026, from https://finance.yahoo.com/quote/TSLA/
- Finnhub. (n.d.). Company news: TSLA. Finnhub. Retrieved April 20, 2026, from https://finnhub.io/docs/api/company-news

---

## Team

| Name | NUID |
|---|---|
| Yuqing Lei | 003158000 |
| Guoyue Liu | 003169341 |
| Wanjing Yang | 002442139 |
| Yingzi Zhou | 002528444 |

*Northeastern University · Khoury College of Computer Sciences · CS 5004 · Spring 2026*
