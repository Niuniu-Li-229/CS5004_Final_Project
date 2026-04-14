package stockreview.model;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Summarizes all data associated with a single stock review session:
 */
public class StockDataModel {

  private final String            ticker;
  private final List<PricePoint>  prices;
  private final List<MarketEvent> events;

  /**
   * Constructs a StockDataModel for the given ticker.
   *
   * @param ticker  the stock ticker symbol, must not be blank
   * @param prices  the list of price points, must not be null
   * @param events  the list of market events, must not be null
   * @throws IllegalArgumentException if any argument is null or ticker is blank
   */
  public StockDataModel(String ticker,
      List<PricePoint> prices,
      List<MarketEvent> events) {
    if (ticker == null || ticker.isBlank())
      throw new IllegalArgumentException("Ticker must not be blank.");
    if (prices == null)
      throw new IllegalArgumentException("Prices list must not be null.");
    if (events == null)
      throw new IllegalArgumentException("Events list must not be null.");

    this.ticker = ticker.toUpperCase();
    this.prices = Collections.unmodifiableList(new ArrayList<>(prices));
    this.events = Collections.unmodifiableList(new ArrayList<>(events));
  }

  /**
   * @return the stock ticker symbol
   * */
  public String getTicker() { return ticker; }

  /**
   * Returns an unmodifiable view of all price points.
   *
   * @return list of {@link PricePoint} objects
   */
  public List<PricePoint> getPrices() { return prices; }

  /**
   * Returns an unmodifiable view of all market events.
   *
   * @return list of {@link MarketEvent} objects
   */
  public List<MarketEvent> getEvents() { return events; }


  /**
   * Returns price points that fall within the given date range (inclusive).
   *
   * @param start the start date (inclusive), must not be null
   * @param end   the end date (inclusive), must not be null
   * @return a new list of matching PricePoint objects
   * @throws IllegalArgumentException if either date is null
   */

  public List<PricePoint> getPricesInRange(LocalDate start, LocalDate end) {
    if (start == null || end == null)
      throw new IllegalArgumentException("Dates must not be null.");
    List<PricePoint> result = new ArrayList<>();
    for (PricePoint p : prices) {
      if (!p.getDate().isBefore(start) && !p.getDate().isAfter(end))
        result.add(p);
    }
    return result;
  }

  /**
   * Returns market events of the given type.
   *
   * @param type the event type to filter by, must not be null
   * @return a new list of matching MarketEvent objects
   * @throws IllegalArgumentException if type is null
   */

  public List<MarketEvent> getEventsByType(EventType type) {
    if (type == null)
      throw new IllegalArgumentException("EventType must not be null.");
    List<MarketEvent> result = new ArrayList<>();
    for (MarketEvent e : events) {
      if (e.isType(type)) result.add(e);
    }
    return result;
  }

  /** @return the total number of price points */
  public int getPriceCount() { return prices.size(); }

  /** @return the total number of market events */
  public int getEventCount() { return events.size(); }

  @Override
  public String toString() {
    return String.format("StockDataModel[ticker=%s, prices=%d, events=%d]",
        ticker, prices.size(), events.size());
  }
}