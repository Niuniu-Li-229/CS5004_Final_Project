import model.*;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricePointTest {

  private static final LocalDate DATE = LocalDate.of(2025, 9, 3);

  // ── Constructor: valid arguments ───────────────────────────────────────────────

  @Test
  void constructor_validArgs_createsObject() {
    PricePoint p = new PricePoint(DATE, 171.06, 172.41, 168.88, 170.62, 171.78, 164_420_000L); // update previousclose price
    assertEquals(DATE,       p.getDate());
    assertEquals(171.06,     p.getOpen(),   1e-9);
    assertEquals(172.41,     p.getHigh(),   1e-9);
    assertEquals(168.88,     p.getLow(),    1e-9);
    assertEquals(170.62,     p.getClose(),  1e-9);
    assertEquals(171.78,     p.getPreviousClose(),  1e-9); // update previousclose price

    assertEquals(164_420_000L, p.getVolume());
  }

  @Test
  void constructor_zeroVolume_isAllowed() {
    // volume = 0 is non-negative, so it must be accepted
    assertDoesNotThrow(() ->
        new PricePoint(DATE, 100.0, 110.0, 90.0, 105.0, 160.0, 0L)); // update previousclose price
  }

  @Test
  void constructor_highEqualsLow_isAllowed() {
    // high == low is valid (flat trading day)
    assertDoesNotThrow(() ->
        new PricePoint(DATE, 100.0, 100.0, 100.0, 100.0, 176.0, 1000L)); // update previousclose price
  }

  // ── Constructor: invalid inputs ───────────────────────────────────────────

  @Test
  void constructor_nullDate_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new PricePoint(null, 100.0, 110.0, 90.0, 105.0, 112.0, 1000L)); // update previousclose price
  }

  @Test
  void constructor_zeroOpen_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new PricePoint(DATE, 0.0, 110.0, 90.0, 105.0, 112.0, 1000L)); // update previousclose price
  }

  @Test
  void constructor_negativeHigh_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new PricePoint(DATE, 100.0, -1.0, 90.0, 105.0, 112.0, 1000L)); // update previousclose price
  }

  @Test
  void constructor_highLessThanLow_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new PricePoint(DATE, 100.0, 89.0, 90.0, 95.0, 100.0, 1000L)); // update previousclose price
  }

  @Test
  void constructor_negativeVolume_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new PricePoint(DATE, 100.0, 110.0, 90.0, 105.0, 112.0,-1L)); // update previousclose price
  }

  // ── Derived calculations ──────────────────────────────────────────────────

  @Test
  void getDailyRange_returnsHighMinusLow() {
    PricePoint p = new PricePoint(DATE, 171.06, 172.41, 168.88, 170.62, 171.78, 1000L); // update previousclose price
    assertEquals(172.41 - 168.88, p.getDailyRange(), 1e-9);
  }

  @Test
  void getMidPrice_returnsAverageOfHighAndLow() {
    PricePoint p = new PricePoint(DATE, 171.06, 172.41, 168.88, 170.62, 171.78, 1000L); // update previousclose price
    assertEquals((172.41 + 168.88) / 2.0, p.getMidPrice(), 1e-9);
  }


  // ── getSummary / toString ─────────────────────────────────────────────────

  @Test
  void getSummary_containsDateAndClose() {
    PricePoint p = new PricePoint(DATE, 171.06, 172.41, 168.88, 170.62, 171.78, 1000L); // update previousclose price
    String s = p.getSummary();
    assertTrue(s.contains(DATE.toString()));
    assertTrue(s.contains("170.62"));
  }

  @Test
  void toString_containsAllFields() {
    PricePoint p = new PricePoint(DATE, 171.06, 172.41, 168.88, 170.62, 171.78, 164_420_000L); // update previousclose price
    String s = p.toString();
    assertTrue(s.contains("2025-09-03"));
    assertTrue(s.contains("171.06"));
    assertTrue(s.contains("172.41"));
  }
}
