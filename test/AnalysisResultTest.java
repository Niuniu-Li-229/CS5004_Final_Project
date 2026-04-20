import model.*;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalysisResultTest {

  private static final LocalDate START = LocalDate.of(2025, 9, 2);
  private static final LocalDate END   = LocalDate.of(2025, 9, 5);

  private AnalysisResult.AnomalyPoint anomaly;

  @BeforeEach
  void setUp() {
    PricePoint p = new PricePoint(LocalDate.of(2025, 9, 5), 168.0, 169.0, 164.0, 167.0, 171.66, 50_000L);
    MarketEvent e = new MarketEvent(LocalDate.of(2025, 9, 5), "Sell-off", "Macro", "WSJ", EventType.MACRO);
    anomaly = new AnalysisResult.AnomalyPoint(p, -2.70, List.of(e), "Macro-driven drop");
  }

  // ── AnalysisResult constructor: valid input ────────────────────────────────

  @Test
  void constructor_validArgs_createsResult() {
    AnalysisResult r = new AnalysisResult("NVDA", START, END, -2.20, List.of(anomaly), "Summary text");
    assertEquals("NVDA",      r.getTicker());
    assertEquals(START,       r.getStartDate());
    assertEquals(END,         r.getEndDate());
    assertEquals(-2.20,       r.getTotalReturn(), 1e-9);
    assertEquals(1,           r.getAnomalyCount());
    assertEquals("Summary text", r.getSummary());
  }

  @Test
  void constructor_tickerIsNormalisedToUpperCase() {
    AnalysisResult r = new AnalysisResult("nvda", START, END, 0, new ArrayList<>(), "s");
    assertEquals("NVDA", r.getTicker());
  }

  @Test
  void constructor_emptyAnomalies_isAllowed() {
    assertDoesNotThrow(() ->
        new AnalysisResult("AAPL", START, END, 0, new ArrayList<>(), "No anomalies"));
  }

  // ── AnalysisResult constructor: invalid inputs ────────────────────────────

  @Test
  void constructor_nullTicker_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new AnalysisResult(null, START, END, 0, new ArrayList<>(), "s"));
  }

  @Test
  void constructor_blankTicker_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new AnalysisResult("", START, END, 0, new ArrayList<>(), "s"));
  }

  @Test
  void constructor_nullStartDate_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new AnalysisResult("NVDA", null, END, 0, new ArrayList<>(), "s"));
  }

  @Test
  void constructor_endBeforeStart_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new AnalysisResult("NVDA", END, START, 0, new ArrayList<>(), "s")); // reversed
  }

  @Test
  void constructor_startEqualsEnd_isAllowed() {
    // single-day analysis is valid
    assertDoesNotThrow(() ->
        new AnalysisResult("NVDA", START, START, 0, new ArrayList<>(), "s"));
  }

  @Test
  void constructor_nullAnomalies_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new AnalysisResult("NVDA", START, END, 0, null, "s"));
  }

  @Test
  void constructor_nullSummary_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new AnalysisResult("NVDA", START, END, 0, new ArrayList<>(), null));
  }

  // ── Unmodifiable list ─────────────────────────────────────────────────────

  @Test
  void getAnomalies_returnsUnmodifiableList() {
    AnalysisResult r = new AnalysisResult("NVDA", START, END, 0, new ArrayList<>(), "s");
    assertThrows(UnsupportedOperationException.class, () ->
        r.getAnomalies().add(anomaly));
  }

  // ── AnomalyPoint: happy path ──────────────────────────────────────────────

  @Test
  void anomalyPoint_validArgs_storesFields() {
    assertEquals(LocalDate.of(2025, 9, 5), anomaly.getDate());
    assertEquals(-2.70, anomaly.getPercentChange(), 1e-9);
    assertEquals(1,     anomaly.getRelatedEvents().size());
    assertEquals("Macro-driven drop", anomaly.getComment());
  }

  @Test
  void anomalyPoint_negativeChange_isGainReturnsFalse() {
    assertFalse(anomaly.isGain());
  }

  @Test
  void anomalyPoint_positiveChange_isGainReturnsTrue() {
    PricePoint p = new PricePoint(LocalDate.of(2025, 9, 3), 171.0, 172.0, 168.0, 170.0, 169.0, 1000L);
    AnalysisResult.AnomalyPoint gain =
        new AnalysisResult.AnomalyPoint(p, 5.5, new ArrayList<>(), "Earnings beat");
    assertTrue(gain.isGain());
  }

  @Test
  void anomalyPoint_zeroChange_isGainReturnsFalse() {
    PricePoint p = new PricePoint(LocalDate.of(2025, 9, 3), 100.0, 100.0, 100.0, 100.0, 100.0, 0L);
    AnalysisResult.AnomalyPoint flat =
        new AnalysisResult.AnomalyPoint(p, 0.0, new ArrayList<>(), "Flat day");
    assertFalse(flat.isGain()); // 0 is not > 0
  }

  // ── AnomalyPoint: invalid inputs ─────────────────────────────────────────

  @Test
  void anomalyPoint_nullPricePoint_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new AnalysisResult.AnomalyPoint(null, -2.0, new ArrayList<>(), "comment"));
  }

  @Test
  void anomalyPoint_nullRelatedEvents_throwsIllegalArgument() {
    PricePoint p = new PricePoint(LocalDate.of(2025, 9, 3), 100.0, 110.0, 90.0, 105.0, 99.0, 1000L);
    assertThrows(IllegalArgumentException.class, () ->
        new AnalysisResult.AnomalyPoint(p, 1.0, null, "comment"));
  }

  @Test
  void anomalyPoint_nullComment_throwsIllegalArgument() {
    PricePoint p = new PricePoint(LocalDate.of(2025, 9, 3), 100.0, 110.0, 90.0, 105.0, 99.0, 1000L);
    assertThrows(IllegalArgumentException.class, () ->
        new AnalysisResult.AnomalyPoint(p, 1.0, new ArrayList<>(), null));
  }

  @Test
  void anomalyPoint_relatedEvents_returnsUnmodifiableList() {
    assertThrows(UnsupportedOperationException.class, () ->
        anomaly.getRelatedEvents().clear());
  }

  // ── toString ─────────────────────────────────────────────────────────────

  @Test
  void analysisResult_toString_containsKeyFields() {
    AnalysisResult r = new AnalysisResult("NVDA", START, END, -2.20, List.of(anomaly), "s");
    String s = r.toString();
    assertTrue(s.contains("NVDA"));
    assertTrue(s.contains("-2.20"));
  }

  @Test
  void anomalyPoint_toString_containsDateAndChange() {
    String s = anomaly.toString();
    assertTrue(s.contains("2025-09-05"));
    assertTrue(s.contains("-2.70"));
  }
}
