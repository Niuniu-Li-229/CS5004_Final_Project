package stockreview.controller;

import stockreview.model.EventType;
import stockreview.model.MarketEvent;
import stockreview.model.PricePoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for loading stock price and news event data from CSV files.
 * Handles OHLCV price data and news headlines with automatic EventType
 * classification based on keyword analysis of headline and summary text.
 */
public class CsvLoader {

  /** Length of the ISO date prefix to extract (e.g., "2010-06-29"). */
  private static final int DATE_PREFIX_LEN = 10;

  private CsvLoader() {} // utility class — not instantiable

  /**
   * Loads price data from a CSV file. Expected columns:
   * Date, Open, High, Low, Close, Volume [, optional extra columns]
   * The Date field may be "2010-06-29 00:00:00-04:00" or plain "2024-01-02".
   *
   * @param file the CSV file to read, must not be null
   * @return a list of PricePoint objects (malformed rows are silently skipped)
   * @throws IOException              if the file cannot be read
   * @throws IllegalArgumentException if file is null
   */
  public static List<PricePoint> loadPrices(File file) throws IOException {
    if (file == null) throw new IllegalArgumentException("File must not be null.");
    List<PricePoint> result = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String header = reader.readLine(); // skip header row
      if (header == null) return result;
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) continue;
        try {
          PricePoint pp = parsePriceLine(line);
          if (pp != null) result.add(pp);
        } catch (Exception ignored) {
          // skip rows that cannot be parsed
        }
      }
    }
    return result;
  }

  /**
   * Loads market event data from a CSV file. Expected columns:
   * date, datetime_unix, headline, summary, source, url, category
   *
   * @param file the CSV file to read, must not be null
   * @return a list of MarketEvent objects (malformed rows are silently skipped)
   * @throws IOException              if the file cannot be read
   * @throws IllegalArgumentException if file is null
   */
  public static List<MarketEvent> loadEvents(File file) throws IOException {
    if (file == null) throw new IllegalArgumentException("File must not be null.");
    List<MarketEvent> result = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String header = reader.readLine();
      if (header == null) return result;
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) continue;
        try {
          MarketEvent ev = parseEventLine(line);
          if (ev != null) result.add(ev);
        } catch (Exception ignored) {
          // skip rows that cannot be parsed
        }
      }
    }
    return result;
  }

  // ── private parsing helpers ────────────────────────────────────────────────

  /** Parses one price CSV row into a PricePoint, or returns null if invalid. */
  private static PricePoint parsePriceLine(String line) {
    String[] parts = splitCsvLine(line);
    if (parts.length < 6) return null;
    LocalDate date = parseDate(parts[0].trim());
    if (date == null) return null;
    double open  = Double.parseDouble(parts[1].trim());
    double high  = Double.parseDouble(parts[2].trim());
    double low   = Double.parseDouble(parts[3].trim());
    double close = Double.parseDouble(parts[4].trim());
    long   vol   = (long) Double.parseDouble(parts[5].trim());
    return new PricePoint(date, open, high, low, close, vol);
  }

  /** Parses one news CSV row into a MarketEvent, or returns null if invalid. */
  private static MarketEvent parseEventLine(String line) {
    String[] parts = splitCsvLine(line);
    if (parts.length < 5) return null;
    LocalDate date = parseDate(parts[0].trim());
    if (date == null) return null;
    String headline    = unquote(parts[2].trim());
    String description = unquote(parts[3].trim());
    String source      = unquote(parts[4].trim());
    if (headline.isBlank()) return null;
    if (source.isBlank())   source = "Unknown";
    EventType type = inferEventType(headline + " " + description);
    return new MarketEvent(date, headline, description, source, type);
  }

  /**
   * Parses a date string that may be "2010-06-29 00:00:00-04:00" or "2024-01-02".
   * Returns null if parsing fails.
   */
  private static LocalDate parseDate(String raw) {
    try {
      String d = raw.length() > DATE_PREFIX_LEN ? raw.substring(0, DATE_PREFIX_LEN) : raw;
      return LocalDate.parse(d);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Infers an EventType from combined headline and description text using
   * keyword matching. Priority order: EARNINGS > ANALYST > REGULATORY >
   * MACRO > PRODUCT > OTHER.
   *
   * @param text combined headline and description (may be null)
   * @return the best-matching EventType
   */
  static EventType inferEventType(String text) {
    if (text == null) return EventType.OTHER;
    String lower = text.toLowerCase();
    if (containsAny(lower, "earnings", " eps ", "revenue", "quarterly",
        " beat ", " miss ", " q1 ", " q2 ", " q3 ", " q4 ", "profit", "net loss"))
      return EventType.EARNINGS;
    if (containsAny(lower, "analyst", "upgrade", "downgrade", "rating",
        "price target", "initiates", "reiterates", "overweight", "underweight",
        "buy rating", "sell rating", "hold rating"))
      return EventType.ANALYST;
    if (containsAny(lower, "regulat", "sec ", "court", "fine ", "penalty",
        "antitrust", "fda", "law", "legal", "lawsuit", "investigation", "probe"))
      return EventType.REGULATORY;
    if (containsAny(lower, "fed ", "federal reserve", "interest rate",
        "inflation", " gdp", "recession", "tariff", "dow jones", "s&p 500",
        "market gains", "market falls", "macro"))
      return EventType.MACRO;
    if (containsAny(lower, "product", "launch", "model ", "vehicle",
        "software", "update", "release", "delivery", "robotaxi",
        "autopilot", "supercharger", "gigafactory", "driverless"))
      return EventType.PRODUCT;
    return EventType.OTHER;
  }

  /** Returns true if text contains any of the given keywords. */
  private static boolean containsAny(String text, String... keywords) {
    for (String kw : keywords) {
      if (text.contains(kw)) return true;
    }
    return false;
  }

  /** Removes surrounding double-quotes from a CSV field value. */
  private static String unquote(String s) {
    if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\""))
      return s.substring(1, s.length() - 1).replace("\"\"", "\"");
    return s;
  }

  /**
   * Splits a CSV line into fields, respecting double-quoted fields that may
   * contain commas or escaped quotes.
   *
   * @param line the raw CSV line
   * @return array of field strings (not unquoted; call unquote() separately)
   */
  static String[] splitCsvLine(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        // escaped quote inside quoted field
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        fields.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    fields.add(current.toString());
    return fields.toArray(new String[0]);
  }
}
