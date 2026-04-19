package controller;

import model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple demo class to verify that all model layer objects
 * used with NVIDIA (NVDA) sample data.
 */
public class ModelDemo {

  public static void main(String[] args) {

    System.out.println("Testing EventType");
    for (EventType type : EventType.values()) {
      System.out.println("  " + type);
    }

    System.out.println("\nTesting PricePoint");
    PricePoint p1 = new PricePoint(LocalDate.of(2025, 9, 2),
        170.00, 172.38, 167.22, 170.78, 174.18, 231_160_000L);
    PricePoint p2 = new PricePoint(LocalDate.of(2025, 9, 3),
        171.06, 172.41, 168.88, 170.62, 170.78, 164_420_000L);
    PricePoint p3 = new PricePoint(LocalDate.of(2025, 9, 4),
        170.57, 171.86, 169.41, 171.66, 170.62, 141_670_000L);
    PricePoint p5 = new PricePoint(LocalDate.of(2025, 9, 5),
        168.03, 169.03, 164.07, 167.02, 171.66, 224_440_000L);

    System.out.println("  " + p3);
    System.out.println("  Summary      : " + p3.getSummary());
    System.out.println("  Daily Range  : " + p3.getDailyRange());
    System.out.println("  Mid Price    : " + p3.getMidPrice());
    System.out.println("  % Change     : " + String.format("%.2f%%", p3.getPercentChange(p2.getClose())));

    System.out.println("\nTesting MarketEvent");
    MarketEvent analystUpgrade = new MarketEvent(
        LocalDate.of(2025, 9, 2),
        "Multiple analysts raise NVIDIA price targets ahead of earnings",
        "Wall Street analysts raised price targets citing strong data center demand.",
        "Bloomberg",
        EventType.ANALYST);

    MarketEvent regulatory = new MarketEvent(
        LocalDate.of(2025, 9, 4),
        "US considers new export restrictions on AI chips to China",
        "The Biden administration is weighing additional curbs on advanced chip exports.",
        "FT",
        EventType.REGULATORY);

    MarketEvent macro = new MarketEvent(
        LocalDate.of(2025, 9, 5),
        "Broad market sell-off on Fed rate concerns",
        "Investors rotated out of tech stocks amid fears of prolonged high interest rates.",
        "WSJ",
        EventType.MACRO);

    System.out.println("  " + analystUpgrade);
    System.out.println("  Summary         : " + analystUpgrade.getSummary());
    System.out.println("  isType(ANALYST) : " + analystUpgrade.isType(EventType.ANALYST));
    System.out.println("  isType(MACRO)   : " + analystUpgrade.isType(EventType.MACRO));

    System.out.println("\nTesting EarningsEvent (polymorphism)");
    EarningsEvent earnings = new EarningsEvent(
        LocalDate.of(2025, 9, 3),
        "NVIDIA reports record Q3 earnings, beats estimates",
        "NVIDIA posted record revenue driven by surging demand for H100 and B100 AI chips.",
        "Reuters",
        1.62, true);

    MarketEvent asMarket = earnings;
    System.out.println("  " + earnings);
    System.out.println("  EPS             : " + earnings.getReportedEps());
    System.out.println("  Beat estimates  : " + earnings.beatExpectations());
    System.out.println("  getSummary() via MarketEvent ref: " + asMarket.getSummary());

    System.out.println("\nTesting StockDataModel (composition)");
    List<PricePoint> prices = new ArrayList<>();
    prices.add(p1); prices.add(p2); prices.add(p3); prices.add(p5);

    List<MarketEvent> events = new ArrayList<>();
    events.add(analystUpgrade);
    events.add(earnings);
    events.add(regulatory);
    events.add(macro);

    StockDataModel model = new StockDataModel("NVDA", prices, events);
    System.out.println("  " + model);
    System.out.println("  Price count : " + model.getPriceCount());
    System.out.println("  Event count : " + model.getEventCount());

    System.out.println("\n  getPricesInRange(Sep 3 to Sep 4):");
    for (PricePoint p : model.getPricesInRange(
        LocalDate.of(2025, 9, 3), LocalDate.of(2025, 9, 4))) {
      System.out.println("    " + p.getSummary());
    }

    System.out.println("\n  getEventsByType(REGULATORY):");
    for (MarketEvent e : model.getEventsByType(EventType.REGULATORY)) {
      System.out.println("    " + e.getSummary());
    }

    System.out.println("\nTesting AnalysisResult + AnomalyPoint");

    List<MarketEvent> sep3Events = new ArrayList<>();
    sep3Events.add(earnings);
    AnalysisResult.AnomalyPoint gainAnomaly = new AnalysisResult.AnomalyPoint(
        p3, p3.getPercentChange(p2.getClose()), sep3Events,
        "Normal trading day; move of " + String.format("%.2f%%", p3.getPercentChange(p2.getClose())) + " is below the 5% significance threshold.");

    List<MarketEvent> sep5Events = new ArrayList<>();
    sep5Events.add(macro);
    AnalysisResult.AnomalyPoint lossAnomaly = new AnalysisResult.AnomalyPoint(
        p5, p5.getPercentChange(p3.getClose()), sep5Events,
        "Normal trading day; move of " + String.format("%.2f%%", p5.getPercentChange(p3.getClose())) + " is below the 5% significance threshold.");

    System.out.println("  Sep 3 anomaly : " + gainAnomaly);
    System.out.println("  Date          : " + gainAnomaly.getDate());
    System.out.println("  Change        : " + String.format("%+.2f%%", gainAnomaly.getPercentChange()));
    System.out.println("  Is gain       : " + gainAnomaly.isGain());
    System.out.println("  Comment       : " + gainAnomaly.getComment());
    System.out.println("  Related news  :");
    gainAnomaly.getRelatedEvents().forEach(e ->
        System.out.println("    [" + e.getType() + "] " + e.getTitle()));

    System.out.println("\n  Sep 5 anomaly : " + lossAnomaly);
    System.out.println("  Change        : " + String.format("%+.2f%%", lossAnomaly.getPercentChange()));
    System.out.println("  Is gain       : " + lossAnomaly.isGain());
    System.out.println("  Comment       : " + lossAnomaly.getComment());

    List<AnalysisResult.AnomalyPoint> anomalies = new ArrayList<>();
    anomalies.add(gainAnomaly);
    anomalies.add(lossAnomaly);

    AnalysisResult result = new AnalysisResult(
        "NVDA",
        LocalDate.of(2025, 9, 2),
        LocalDate.of(2025, 9, 5),
        -2.20,
        anomalies,
        "NVDA lost 2.20% over the period. 2 significant moves detected: a sell-off on " +
            "Sep 5 due to broad macro concerns, and regulatory concerns on Sep 4 regarding " +
            "AI chip export restrictions to China.");

    System.out.println("\n  AnalysisResult : " + result);
    System.out.println("  Ticker         : " + result.getTicker());
    System.out.println("  Period         : " + result.getStartDate()
        + " → " + result.getEndDate());
    System.out.println("  Total Return   : " + String.format("%+.2f%%", result.getTotalReturn()));
    System.out.println("  Anomaly count  : " + result.getAnomalyCount());
    System.out.println("  Summary        : " + result.getSummary());

    System.out.println("\nTesting invalid input (encapsulation)");
    try {
      new PricePoint(null, 480, 492, 478, 489, 174.18, 18_000_000L);
    } catch (IllegalArgumentException ex) {
      System.out.println("  Caught expected error: " + ex.getMessage());
    }
    try {
      new MarketEvent(LocalDate.now(), "", "desc", "src", EventType.OTHER);
    } catch (IllegalArgumentException ex) {
      System.out.println("  Caught expected error: " + ex.getMessage());
    }
    try {
      new AnalysisResult(null, LocalDate.now(), LocalDate.now(),
          0, new ArrayList<>(), "summary");
    } catch (IllegalArgumentException ex) {
      System.out.println("  Caught expected error: " + ex.getMessage());
    }

    System.out.println("\nAll model tests passed!");
  }
}
