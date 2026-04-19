package controller;

import model.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller layer for the stock analysis application. Coordinates data
 * loading via {@link CsvLoader}, maintains the {@link StockDataModel}, and
 * produces {@link AnalysisResult} objects on demand. Keeps the view classes
 * free of business logic by centralising all decisions here.
 */
public class StockController {

  /** Default threshold (percent) above which a daily move is flagged as anomalous. */
  public static final double DEFAULT_THRESHOLD = 5.0;

  /** Number of calendar days on each side of a price date to search for related events. */
  private static final int EVENT_WINDOW_DAYS = 3;

  private StockDataModel   model;
  private AnalysisResult   lastResult;

  /**
   * Constructs an empty controller. Call {@link #loadData} before calling
   * any analysis or query methods.
   */
  public StockController() {
    this.model      = null;
    this.lastResult = null;
  }

  /**
   * Reads price and news CSV files and builds the internal {@link StockDataModel}.
   * This method is blocking; callers that need a responsive UI should invoke it
   * from a background thread (e.g., SwingWorker).
   *
   * @param priceCsv absolute or relative path to the price CSV file
   * @param newsCsv  absolute or relative path to the news CSV file
   * @throws IOException              if either file cannot be read
   * @throws IllegalArgumentException if either path is null or blank
   */
  public void loadData(String priceCsv, String newsCsv) throws IOException {
    List<PricePoint>  prices = CsvLoader.loadPrices(priceCsv);
    List<MarketEvent> events = CsvLoader.loadEvents(newsCsv);
    this.model = new StockDataModel(parseTicker(priceCsv), prices, events);
    this.lastResult = null;
  }

  /**
   * Extracts a ticker symbol from the price CSV filename.
   * Expects a name like "AAPL_price.csv"; falls back to "STOCK" if the
   * filename does not follow that convention.
   *
   * @param priceCsv the price CSV path
   * @return the upper-cased ticker symbol, never null or blank
   */
  private static String parseTicker(String priceCsv) {
    String name = priceCsv.replaceAll(".*[/\\\\]", "");
    int underscore = name.indexOf('_');
    int dot        = name.lastIndexOf('.');
    if (underscore > 0) return name.substring(0, underscore).toUpperCase();
    if (dot        > 0) return name.substring(0, dot).toUpperCase();
    return "STOCK";
  }

  /**
   * Returns whether price and news data have been successfully loaded.
   *
   * @return true if a model is available, false otherwise
   */
  public boolean isDataLoaded() {
    return model != null;
  }

  /**
   * Returns the underlying data model. Callers should first check
   * {@link #isDataLoaded()}.
   *
   * @return the current {@link StockDataModel}, or null if not yet loaded
   */
  public StockDataModel getModel() {
    return model;
  }

  /**
   * Returns the result of the most recent {@link #analyze} call, or null
   * if no analysis has been run yet.
   *
   * @return the last {@link AnalysisResult}, possibly null
   */
  public AnalysisResult getLastResult() {
    return lastResult;
  }

  /**
   * Analyses TSLA price data over the given date range and identifies
   * anomaly days — days where the absolute daily percent change meets or
   * exceeds {@code threshold}. For each anomaly the controller attaches any
   * news events published within {@value #EVENT_WINDOW_DAYS} days of that date.
   *
   * @param start     start of the analysis period (inclusive), must not be null
   * @param end       end of the analysis period (inclusive), must not be null
   * @param threshold minimum absolute percent change to flag as anomalous
   * @return an {@link AnalysisResult} with anomalies, total return, and summary
   * @throws IllegalStateException    if data has not been loaded
   * @throws IllegalArgumentException if start or end is null, or end is before start
   */
  public AnalysisResult analyze(LocalDate start, LocalDate end, double threshold) {
    if (!isDataLoaded())
      throw new IllegalStateException("Data not loaded. Call loadData() first.");
    if (start == null || end == null)
      throw new IllegalArgumentException("Dates must not be null.");
    if (end.isBefore(start))
      throw new IllegalArgumentException("End date must not be before start date.");

    List<PricePoint>  prices = model.getPricesInRange(start, end);
    List<MarketEvent> events = model.getEvents();

    if (prices.isEmpty()) {
      lastResult = new AnalysisResult(model.getTicker(), start, end, 0.0,
          new ArrayList<>(), "No price data found in the selected range.");
      return lastResult;
    }

    double startClose = prices.get(0).getClose();
    double endClose   = prices.get(prices.size() - 1).getClose();
    double totalReturn = (endClose - startClose) / startClose * 100.0;

    List<AnalysisResult.AnomalyPoint> anomalies = new ArrayList<>();
    for (int i = 1; i < prices.size(); i++) {
      PricePoint current  = prices.get(i);
      double     prevClose = prices.get(i - 1).getClose();
      double     pct      = current.getPercentChange(prevClose);

      if (Math.abs(pct) >= threshold) {
        List<MarketEvent> related = findRelatedEvents(events, current.getDate());
        String comment = buildComment(pct, related);
        anomalies.add(new AnalysisResult.AnomalyPoint(current, pct, related, comment));
      }
    }

    String ticker = model.getTicker();
    String summary = String.format(
        "%s  %s → %s  |  Total return: %+.2f%%  |  "
        + "Anomaly days (≥%.1f%%): %d  |  Price records: %d  |  News events: %d",
        ticker, start, end, totalReturn, threshold, anomalies.size(),
        prices.size(), events.size());

    lastResult = new AnalysisResult(ticker, start, end, totalReturn, anomalies, summary);
    return lastResult;
  }

  /**
   * Returns all market events within {@value #EVENT_WINDOW_DAYS} calendar days
   * of the given date (inclusive on both sides).
   *
   * @param events all available events, must not be null
   * @param date   the reference date, must not be null
   * @return a new list of nearby events, never null
   */
  private List<MarketEvent> findRelatedEvents(List<MarketEvent> events, LocalDate date) {
    List<MarketEvent> related = new ArrayList<>();
    for (MarketEvent e : events) {
      long diff = Math.abs(e.getDate().toEpochDay() - date.toEpochDay());
      if (diff <= EVENT_WINDOW_DAYS) related.add(e);
    }
    return related;
  }

  /**
   * Builds a human-readable explanation for an anomaly point.
   * Includes the first two related event headlines when available.
   *
   * @param pct     daily percent change (positive = gain)
   * @param related list of nearby news events
   * @return a non-null, non-empty explanation string
   */
  private String buildComment(double pct, List<MarketEvent> related) {
    String dir = pct > 0 ? "gained" : "dropped";
    StringBuilder sb = new StringBuilder(
        String.format("Price %s %.2f%%.", dir, Math.abs(pct)));
    if (!related.isEmpty()) {
      sb.append(" Nearby news: ");
      for (int i = 0; i < Math.min(2, related.size()); i++) {
        if (i > 0) sb.append(" | ");
        sb.append(related.get(i).getTitle());
      }
      if (related.size() > 2)
        sb.append(String.format(" (+ %d more)", related.size() - 2));
    } else {
      sb.append(" No nearby news found.");
    }
    return sb.toString();
  }
}
