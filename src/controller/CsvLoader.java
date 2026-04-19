package controller;

import model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that reads TSLA price and news CSV files and converts
 * each row into the corresponding model objects ({@link PricePoint} and
 * {@link MarketEvent}). Malformed rows are silently skipped so that one
 * bad record does not abort the entire load.
 */
public class CsvLoader {

  /**
   * Loads daily price data from a CSV file whose columns are:
   * Date, Open, High, Low, Close, Volume, PercentChange.
   * The Date column may include a time-zone suffix; only the first 10
   * characters (YYYY-MM-DD) are used.
   *
   * @param csvPath path to the price CSV file, must not be null or blank
   * @return an ordered list of {@link PricePoint} objects, never null
   * @throws IOException              if the file cannot be read
   * @throws IllegalArgumentException if csvPath is null or blank
   */
  public static List<PricePoint> loadPrices(String csvPath) throws IOException {
    if (csvPath == null || csvPath.isBlank())
      throw new IllegalArgumentException("csvPath must not be blank.");

    List<String> lines  = Files.readAllLines(Paths.get(csvPath));
    List<PricePoint> result = new ArrayList<>();
    double prevClose = -1;

    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      if (line.isEmpty()) continue;
      String[] cols = line.split(",");
      if (cols.length < 6) continue;

      try {
        LocalDate date   = LocalDate.parse(cols[0].trim().substring(0, 10));
        double    open   = Double.parseDouble(cols[1].trim());
        double    high   = Double.parseDouble(cols[2].trim());
        double    low    = Double.parseDouble(cols[3].trim());
        double    close  = Double.parseDouble(cols[4].trim());
        long      volume = (long) Double.parseDouble(cols[5].trim());

        // Use the previous row's close as previousClose; for the very first
        // row we fall back to the row's own close so the constructor stays valid.
        double pc = (prevClose > 0) ? prevClose : close;

        result.add(new PricePoint(date, open, high, low, close, pc, volume));
        prevClose = close;
      } catch (Exception ignored) {
        // Skip rows that cannot be parsed (header remnants, etc.)
      }
    }
    return result;
  }

  /**
   * Loads news/event data from a CSV file whose columns are:
   * date, datetime_unix, headline, summary, source, url, category.
   * Quoted fields containing commas are handled correctly. The headline
   * is classified into an {@link EventType} via keyword matching.
   *
   * @param csvPath path to the news CSV file, must not be null or blank
   * @return a list of {@link MarketEvent} objects, never null
   * @throws IOException              if the file cannot be read
   * @throws IllegalArgumentException if csvPath is null or blank
   */
  public static List<MarketEvent> loadEvents(String csvPath) throws IOException {
    if (csvPath == null || csvPath.isBlank())
      throw new IllegalArgumentException("csvPath must not be blank.");

    List<String> lines = Files.readAllLines(Paths.get(csvPath));
    List<MarketEvent> result = new ArrayList<>();

    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      if (line.isEmpty()) continue;
      String[] cols = splitCsvLine(line);
      if (cols.length < 5) continue;

      try {
        LocalDate date     = LocalDate.parse(cols[0].trim().substring(0, 10));
        String    headline = strip(cols[2]);
        String    summary  = strip(cols[3]);
        String    source   = strip(cols[4]);

        if (headline.isBlank()) continue;
        if (source.isBlank())   source = "Unknown";

        EventType type = classifyEvent(headline);
        result.add(new MarketEvent(date, headline, summary, source, type));
      } catch (Exception ignored) {
        // Skip rows that cannot be parsed
      }
    }
    return result;
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  /**
   * Classifies a news headline into an {@link EventType} using keyword matching.
   *
   * @param headline the article headline, must not be null
   * @return the best-matching EventType
   */
  private static EventType classifyEvent(String headline) {
    String h = headline.toLowerCase();
    if (h.contains("earn") || h.contains("eps")    || h.contains("profit")
        || h.contains("revenue") || h.contains("quarter"))      return EventType.EARNINGS;
    if (h.contains("analyst") || h.contains("upgrade")
        || h.contains("downgrade") || h.contains("target")
        || h.contains("rating") || h.contains("price target"))  return EventType.ANALYST;
    if (h.contains("sec")    || h.contains("regul")
        || h.contains("lawsuit") || h.contains("recall")
        || h.contains("investigation") || h.contains("fine"))   return EventType.REGULATORY;
    if (h.contains("fed")    || h.contains("rate")
        || h.contains("inflation") || h.contains("gdp")
        || h.contains("dow jones") || h.contains("s&p 500")
        || h.contains("nasdaq"))                                 return EventType.MACRO;
    if (h.contains("model")  || h.contains("autopilot")
        || h.contains("cybertruck") || h.contains("battery")
        || h.contains("robotaxi")   || h.contains("delivery")
        || h.contains("launch")     || h.contains("vehicle")
        || h.contains("supercharger"))                           return EventType.PRODUCT;
    return EventType.OTHER;
  }

  /**
   * Splits a single CSV line respecting double-quoted fields that may
   * contain commas.
   *
   * @param line a raw CSV line, must not be null
   * @return array of field strings with surrounding quotes removed
   */
  private static String[] splitCsvLine(String line) {
    List<String> tokens = new ArrayList<>();
    StringBuilder sb    = new StringBuilder();
    boolean inQuotes    = false;

    for (char c : line.toCharArray()) {
      if (c == '"') {
        inQuotes = !inQuotes;
      } else if (c == ',' && !inQuotes) {
        tokens.add(sb.toString());
        sb.setLength(0);
      } else {
        sb.append(c);
      }
    }
    tokens.add(sb.toString());
    return tokens.toArray(new String[0]);
  }

  /**
   * Strips leading/trailing whitespace and surrounding double-quotes from a field.
   *
   * @param raw the raw CSV field value
   * @return cleaned string
   */
  private static String strip(String raw) {
    return raw.trim().replaceAll("^\"|\"$", "");
  }
}
