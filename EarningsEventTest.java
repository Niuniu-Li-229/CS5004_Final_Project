import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EarningsEventTest {

  private static final LocalDate DATE = LocalDate.of(2025, 9, 3);

  @Test
  void constructor_validArgs_setsEarningsFields() {
    EarningsEvent e = new EarningsEvent(DATE, "NVDA Earnings", "Strong Q3", "Reuters", 1.62, true);
    assertEquals(1.62, e.getReportedEps(), 1e-9);
    assertTrue(e.beatExpectations());
    assertEquals(EventType.EARNINGS, e.getType());
  }

  @Test
  void constructor_missedExpectations_storesFalse() {
    EarningsEvent e = new EarningsEvent(DATE, "Title", "Desc", "Src", 0.50, false);
    assertFalse(e.beatExpectations());
  }

  // ── Polymorphism: getSummary override ────────────────────────────────────

  @Test
  void getSummary_beat_containsBeatAndEps() {
    EarningsEvent e = new EarningsEvent(DATE, "NVDA", "Desc", "Reuters", 1.62, true);
    String s = e.getSummary();
    assertTrue(s.contains("BEAT"));
    assertTrue(s.contains("1.62"));
  }

  @Test
  void getSummary_missed_containsMissed() {
    EarningsEvent e = new EarningsEvent(DATE, "NVDA", "Desc", "Reuters", 0.50, false);
    assertTrue(e.getSummary().contains("MISSED"));
  }

  @Test
  void getSummary_viaMarketEventRef_usesOverride() {
    // Polymorphism: the overridden getSummary() should be called even via a MarketEvent reference
    MarketEvent ref = new EarningsEvent(DATE, "NVDA", "Desc", "Reuters", 1.62, true);
    assertTrue(ref.getSummary().contains("EPS"));
  }
}