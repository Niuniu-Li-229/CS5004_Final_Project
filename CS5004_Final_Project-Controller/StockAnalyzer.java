package stockreview.controller;

import stockreview.model.AnalysisResult;
import stockreview.model.AnalysisResult.AnomalyPoint;
import stockreview.model.MarketEvent;
import stockreview.model.PricePoint;
import stockreview.model.StockDataModel;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs statistical anomaly detection on a StockDataModel over a date range.
 * A trading day is flagged as anomalous when its open-to-close percent change
 * exceeds a computed threshold. Each anomaly is correlated with nearby market
 * events to produce an AnalysisResult.
 */
public class StockAnalyzer {

  /** Minimum absolute percent change required to flag a day as an anomaly. */
  private static final double MIN_ANOMALY_PCT = 3.0;

  /** Z-score multiplier used when computing the dynamic threshold. */
  private static final double STD_MULTIPLIER = 1.5;

  /** Days before and after an anomaly to search for related market events. */
  private static final int EVENT_WINDOW_DAYS = 3;

  private StockAnalyzer() {} // utility class — not instantiable

  /**
   * Analyzes price and event data for a given date range and returns an
   * AnalysisResult containing detected anomalies and a summary.
   *
   * @param model the stock data model with price and event data, must not be null
   * @param start start date (inclusive), must not be null
   * @param end   end date (inclusive), must not be null
   * @return an AnalysisResult for the requested period
   * @throws IllegalArgumentException if any argument is null or start is after end
   */
  public static AnalysisResult analyze(StockDataModel model, LocalDate start, LocalDate end) {
    if (model == null) throw new IllegalArgumentException("Model must not be null.");
    if (start == null || end == null) throw new IllegalArgumentException("Dates must not be null.");
    if (start.isAfter(end)) throw new IllegalArgumentException("Start must not be after end.");

    List<PricePoint>  prices = model.getPricesInRange(start, end);
    List<MarketEvent> events = model.getEvents();

    double totalReturn = computeTotalReturn(prices);
    double threshold   = computeThreshold(prices);
    List<AnomalyPoint> anomalies = detectAnomalies(prices, events, threshold);

    String summary = buildSummary(model.getTicker(), start, end,
        prices.size(), totalReturn, anomalies.size(), threshold);

    return new AnalysisResult(model.getTicker(), start, end, totalReturn, anomalies, summary);
  }

  // ── private helpers ────────────────────────────────────────────────────────

  /**
   * Computes total percent return over the price list:
   * (last close − first close) / first close × 100.
   * Returns 0.0 if fewer than two price points are present.
   */
  private static double computeTotalReturn(List<PricePoint> prices) {
    if (prices.size() < 2) return 0.0;
    double first = prices.get(0).getClose();
    double last  = prices.get(prices.size() - 1).getClose();
    return (last - first) / first * 100.0;
  }

  /**
   * Computes the anomaly detection threshold as the larger of:
   * MIN_ANOMALY_PCT and (mean + STD_MULTIPLIER × stdDev) of absolute daily changes.
   */
  private static double computeThreshold(List<PricePoint> prices) {
    if (prices.size() < 2) return MIN_ANOMALY_PCT;

    double[] absChanges = new double[prices.size()];
    for (int i = 0; i < prices.size(); i++)
      absChanges[i] = Math.abs(prices.get(i).getOpenToCloseChange());

    double mean = 0;
    for (double c : absChanges) mean += c;
    mean /= absChanges.length;

    double variance = 0;
    for (double c : absChanges) variance += (c - mean) * (c - mean);
    double stdDev = Math.sqrt(variance / absChanges.length);

    return Math.max(MIN_ANOMALY_PCT, mean + STD_MULTIPLIER * stdDev);
  }

  /**
   * Scans the price list for days whose absolute open-to-close change meets
   * or exceeds the threshold, then correlates each with nearby events.
   */
  private static List<AnomalyPoint> detectAnomalies(
      List<PricePoint> prices, List<MarketEvent> events, double threshold) {

    List<AnomalyPoint> anomalies = new ArrayList<>();
    for (PricePoint p : prices) {
      double change = p.getOpenToCloseChange();
      if (Math.abs(change) >= threshold) {
        List<MarketEvent> nearby = findNearbyEvents(p.getDate(), events);
        String comment = buildComment(change, nearby);
        anomalies.add(new AnomalyPoint(p, change, nearby, comment));
      }
    }
    return anomalies;
  }

  /**
   * Returns all events whose date is within EVENT_WINDOW_DAYS of the given date.
   */
  private static List<MarketEvent> findNearbyEvents(LocalDate date, List<MarketEvent> events) {
    List<MarketEvent> nearby = new ArrayList<>();
    for (MarketEvent e : events) {
      long diff = Math.abs(ChronoUnit.DAYS.between(date, e.getDate()));
      if (diff <= EVENT_WINDOW_DAYS) nearby.add(e);
    }
    return nearby;
  }

  /** Builds a human-readable explanation for one anomalous trading day. */
  private static String buildComment(double change, List<MarketEvent> events) {
    String direction = change > 0 ? "gained" : "dropped";
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("Stock %s %.1f%% on this day.", direction, Math.abs(change)));
    if (events.isEmpty()) {
      sb.append(" No nearby news events found.");
    } else {
      sb.append(String.format(" %d related event(s) found nearby:", events.size()));
      for (MarketEvent e : events)
        sb.append("\n  \u2022 [").append(e.getType()).append("] ").append(e.getTitle());
    }
    return sb.toString();
  }

  /** Builds the plain-text summary line stored in the AnalysisResult. */
  private static String buildSummary(String ticker, LocalDate start, LocalDate end,
      int priceCount, double totalReturn, int anomalyCount, double threshold) {
    return String.format(
        "%s | %s \u2192 %s | %d trading days | Total return: %+.1f%% | "
        + "Anomaly threshold: %.1f%% | Anomalies: %d",
        ticker, start, end, priceCount, totalReturn, threshold, anomalyCount);
  }
}
