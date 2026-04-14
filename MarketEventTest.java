import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MarketEventTest {

  private static final LocalDate DATE = LocalDate.of(2025, 9, 2);

  // ── Constructor: valid inputs ───────────────────────────────────────────────

  @Test
  void constructor_validArgs_createsObject() {
    MarketEvent e = new MarketEvent(DATE, "Title", "Desc", "Bloomberg", EventType.ANALYST);
    assertEquals(DATE,            e.getDate());
    assertEquals("Title",         e.getTitle());
    assertEquals("Desc",          e.getDescription());
    assertEquals("Bloomberg",     e.getSource());
    assertEquals(EventType.ANALYST, e.getType());
  }

  // ── Constructor: invalid inputs ───────────────────────────────────────────

  @Test
  void constructor_nullDate_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new MarketEvent(null, "Title", "Desc", "Src", EventType.ANALYST));
  }

  @Test
  void constructor_blankTitle_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new MarketEvent(DATE, "  ", "Desc", "Src", EventType.ANALYST));
  }

  @Test
  void constructor_emptyTitle_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new MarketEvent(DATE, "", "Desc", "Src", EventType.ANALYST));
  }

  @Test
  void constructor_nullDescription_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new MarketEvent(DATE, "Title", null, "Src", EventType.ANALYST));
  }

  @Test
  void constructor_blankSource_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new MarketEvent(DATE, "Title", "Desc", "", EventType.OTHER));
  }

  @Test
  void constructor_nullEventType_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new MarketEvent(DATE, "Title", "Desc", "Src", null));
  }

  // ── isType ────────────────────────────────────────────────────────────────

  @Test
  void isType_matchingType_returnsTrue() {
    MarketEvent e = new MarketEvent(DATE, "T", "D", "S", EventType.REGULATORY);
    assertTrue(e.isType(EventType.REGULATORY));
  }

  @Test
  void isType_nonMatchingType_returnsFalse() {
    MarketEvent e = new MarketEvent(DATE, "T", "D", "S", EventType.REGULATORY);
    assertFalse(e.isType(EventType.MACRO));
  }

  // ── getSummary / toString ─────────────────────────────────────────────────

  @Test
  void getSummary_containsTypeAndTitle() {
    MarketEvent e = new MarketEvent(DATE, "Big news", "Desc", "Reuters", EventType.MACRO);
    String s = e.getSummary();
    assertTrue(s.contains("MACRO"));
    assertTrue(s.contains("Big news"));
    assertTrue(s.contains("Reuters"));
  }
}