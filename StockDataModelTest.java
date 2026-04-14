import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockDataModelTest {

  private List<PricePoint> prices;
  private List<MarketEvent> events;
  private PricePoint p1, p2, p3;
  private MarketEvent analyst, macro;

  @BeforeEach
  void setUp() {
    p1 = new PricePoint(LocalDate.of(2025, 9, 2),  170.00, 172.38, 167.22, 170.78, 100_000L);
    p2 = new PricePoint(LocalDate.of(2025, 9, 3),  171.06, 172.41, 168.88, 170.62, 200_000L);
    p3 = new PricePoint(LocalDate.of(2025, 9, 4),  170.57, 171.84, 169.41, 171.66, 150_000L);

    analyst = new MarketEvent(LocalDate.of(2025, 9, 2), "Analyst Upgrade", "Strong buy", "Bloomberg", EventType.ANALYST);
    macro   = new MarketEvent(LocalDate.of(2025, 9, 4), "Macro Sell-off",  "Fed fears",  "WSJ",       EventType.MACRO);

    prices = new ArrayList<>(List.of(p1, p2, p3));
    events = new ArrayList<>(List.of(analyst, macro));
  }

  // ── Constructor: valid inputs ───────────────────────────────────────────────

  @Test
  void constructor_validArgs_createsModel() {
    StockDataModel m = new StockDataModel("nvda", prices, events);
    assertEquals("NVDA", m.getTicker());  // uppercase normalisation
    assertEquals(3, m.getPriceCount());
    assertEquals(2, m.getEventCount());
  }

  @Test
  void constructor_emptyLists_isAllowed() {
    assertDoesNotThrow(() ->
        new StockDataModel("AAPL", new ArrayList<>(), new ArrayList<>()));
  }

  // ── Constructor: invalid inputs ───────────────────────────────────────────

  @Test
  void constructor_nullTicker_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new StockDataModel(null, prices, events));
  }

  @Test
  void constructor_blankTicker_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new StockDataModel("  ", prices, events));
  }

  @Test
  void constructor_nullPrices_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new StockDataModel("NVDA", null, events));
  }

  @Test
  void constructor_nullEvents_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new StockDataModel("NVDA", prices, null));
  }

  // ── Defensive copy: mutations to originals must not affect model ──────────

  @Test
  void getPrices_returnsUnmodifiableList() {
    StockDataModel m = new StockDataModel("NVDA", prices, events);
    assertThrows(UnsupportedOperationException.class, () ->
        m.getPrices().add(p1));
  }

  @Test
  void constructor_mutatingOriginalList_doesNotAffectModel() {
    StockDataModel m = new StockDataModel("NVDA", prices, events);
    prices.add(new PricePoint(LocalDate.of(2025, 9, 5), 168.0, 169.0, 164.0, 167.0, 50_000L));
    assertEquals(3, m.getPriceCount()); // model still has original 3
  }

  // ── getPricesInRange ──────────────────────────────────────────────────────

  @Test
  void getPricesInRange_exactBoundaries_includesBothEndpoints() {
    StockDataModel m = new StockDataModel("NVDA", prices, events);
    List<PricePoint> result = m.getPricesInRange(
        LocalDate.of(2025, 9, 2), LocalDate.of(2025, 9, 3));
    assertEquals(2, result.size());
    assertTrue(result.contains(p1));
    assertTrue(result.contains(p2));
  }

  @Test
  void getPricesInRange_noMatchingDates_returnsEmptyList() {
    StockDataModel m = new StockDataModel("NVDA", prices, events);
    List<PricePoint> result = m.getPricesInRange(
        LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
    assertTrue(result.isEmpty());
  }

  @Test
  void getPricesInRange_singleDayRange_returnsThatDay() {
    StockDataModel m = new StockDataModel("NVDA", prices, events);
    List<PricePoint> result = m.getPricesInRange(
        LocalDate.of(2025, 9, 3), LocalDate.of(2025, 9, 3));
    assertEquals(1, result.size());
    assertEquals(p2, result.get(0));
  }

  @Test
  void getPricesInRange_nullStart_throwsIllegalArgument() {
    StockDataModel m = new StockDataModel("NVDA", prices, events);
    assertThrows(IllegalArgumentException.class, () ->
        m.getPricesInRange(null, LocalDate.of(2025, 9, 4)));
  }

  @Test
  void getPricesInRange_nullEnd_throwsIllegalArgument() {
    StockDataModel m = new StockDataModel("NVDA", prices, events);
    assertThrows(IllegalArgumentException.class, () ->
        m.getPricesInRange(LocalDate.of(2025, 9, 2), null));
  }

  // ── getEventsByType ───────────────────────────────────────────────────────

  @Test
  void getEventsByType_matchingType_returnsMatchingEvents() {
    StockDataModel m = new StockDataModel("NVDA", prices, events);
    List<MarketEvent> result = m.getEventsByType(EventType.MACRO);
    assertEquals(1, result.size());
    assertEquals(macro, result.get(0));
  }

  @Test
  void getEventsByType_noMatchingType_returnsEmptyList() {
    StockDataModel m = new StockDataModel("NVDA", prices, events);
    assertTrue(m.getEventsByType(EventType.EARNINGS).isEmpty());
  }

  @Test
  void getEventsByType_nullType_throwsIllegalArgument() {
    StockDataModel m = new StockDataModel("NVDA", prices, events);
    assertThrows(IllegalArgumentException.class, () ->
        m.getEventsByType(null));
  }

  // ── toString ─────────────────────────────────────────────────────────────

  @Test
  void toString_containsTickerAndCounts() {
    StockDataModel m = new StockDataModel("NVDA", prices, events);
    String s = m.toString();
    assertTrue(s.contains("NVDA"));
    assertTrue(s.contains("3")); // price count
    assertTrue(s.contains("2")); // event count
  }
}