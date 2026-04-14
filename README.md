# Stock Review — CS5004 Final Project

A Java Swing desktop application for visualizing stock price history and detecting anomalous trading days using statistical analysis.

---

## Features

- **CSV Data Loading** — Load price (OHLCV) and news/events data from CSV files
- **Price Chart** — Interactive line chart with volume bars; hover any point to see OHLCV details
- **Anomaly Detection** — Flags trading days where open-to-close price change exceeds a dynamic threshold (`max(3%, mean + 1.5 × stdDev)`)
- **Anomaly Markers** — Green circles for gain anomalies, red for loss anomalies; click to highlight
- **Event Correlation** — Each anomaly is matched with nearby market events (±3 days)
- **Event Table** — Filterable table of market events color-coded by type (Earnings, Analyst, Regulatory, Macro, Product, Other)
- **Auto-load** — Automatically loads sample TSLA data on startup if found in the project data directory

---

## Project Structure

```
CS5004_final/
├── CS5004_Final_Project-Model/       # Data models (PricePoint, MarketEvent, StockDataModel, AnalysisResult, ...)
├── CS5004_Final_Project-Controller/  # Business logic (StockAnalyzer, CsvLoader, StockReviewController)
├── CS5004_Final_Project-View/        # Swing GUI (MainFrame, PriceChartPanel, AnalysisPanel, InputPanel, EventTablePanel)
├── CS5004_Final_Project-Test/        # JUnit 5 unit tests
├── CS5004_Final_Project-Data/        # Sample data (TSLA_price.csv, TSLA_news.csv)
└── out/                              # Compiled .class files (generated)
```

---

## Requirements

- Java 11 or later (`java` and `javac` on PATH)

---

## Build & Run

### From the command line

```bash
cd ~/CS5004_final

# 1. Compile
javac -d out \
  CS5004_Final_Project-Model/*.java \
  CS5004_Final_Project-Controller/*.java \
  CS5004_Final_Project-View/*.java

# 2. Run
java -cp out stockreview.view.MainFrame
```

### From IntelliJ IDEA

1. Open the project folder in IntelliJ
2. Mark each `*-Model`, `*-Controller`, `*-View` directory as **Sources Root**
3. Run `MainFrame.main()`

---

## Usage

1. **Auto-load** — On startup the app tries to load `TSLA_price.csv` / `TSLA_news.csv` from the `CS5004_Final_Project-Data/` directory automatically
2. **Load custom data** — Click **Load Files** to select your own price CSV and news CSV
3. **Set date range** — Enter start and end dates in `YYYY-MM-DD` format
4. **Analyze** — Click **Analyze** to run anomaly detection for the selected period
5. **Explore anomalies** — Click any entry in the *Anomaly Days* list to highlight the day on the chart and jump to related events in the table
6. **Filter events** — Use the *Filter by Type* dropdown in the event table to narrow by event category

---

## CSV Format

**Price CSV** (`TSLA_price.csv`)

| Column | Format | Example |
|--------|--------|---------|
| Date | `YYYY-MM-DD` or `YYYY-MM-DD HH:MM:SS±TZ` | `2024-01-02` |
| Open | decimal | `248.50` |
| High | decimal | `251.30` |
| Low | decimal | `246.20` |
| Close | decimal | `250.10` |
| Volume | integer | `98234500` |

**News CSV** (`TSLA_news.csv`)

| Column | Format | Example |
|--------|--------|---------|
| Date | `YYYY-MM-DD` | `2024-01-02` |
| Title | string | `Tesla beats Q4 earnings estimates` |
| Source | string | `Reuters` |
| Summary | string (optional) | `...` |

---

## Architecture

The project follows the **MVC** pattern:

- **Model** — Pure data containers; no business logic
- **Controller** — `StockAnalyzer` computes thresholds and detects anomalies; `CsvLoader` parses CSV files; `StockReviewController` coordinates them
- **View** — All Swing panels; user actions call back into `MainFrame`, which delegates to the controller and refreshes panels
