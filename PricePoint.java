package stockreview.model;

import java.time.LocalDate;

/**
 * Represents the open, high, low, close, volume price data
 * for a single trading day.
 */
public class PricePoint extends StockData {

  private final double open;
  private final double high;
  private final double low;
  private final double close;
  private final long   volume;

  /**
   * Constructs a PricePoint with the given open, high, low, close, volume price data.
   *
   * @param date   the trading date, must not be null
   * @param open   opening price, must be positive
   * @param high   highest price of the day, must be >= low
   * @param low    lowest price of the day, must be positive
   * @param close  closing price, must be positive
   * @param volume trading volume, must be non-negative
   * @throws IllegalArgumentException if any value is invalid
   */
  public PricePoint(LocalDate date, double open, double high,
      double low, double close, long volume) {
    super(date);
    requirePositive(open,  "Open");
    requirePositive(high,  "High");
    requirePositive(low,   "Low");
    requirePositive(close, "Close");
    if (high < low)
      throw new IllegalArgumentException("High must be >= low.");
    if (volume < 0)
      throw new IllegalArgumentException("Volume must be non-negative.");

    this.open   = open;
    this.high   = high;
    this.low    = low;
    this.close  = close;
    this.volume = volume;
  }

  /** @return the opening price */
  public double getOpen()  { return open; }

  /** @return the highest price of the day */
  public double getHigh()  { return high; }

  /** @return the lowest price of the day */
  public double getLow()   { return low; }

  /** @return the closing price */
  public double getClose() { return close; }

  /** @return the trading volume */
  public long getVolume()  { return volume; }


  /**
   * Returns the price range for the day (high minus low).
   * @return the difference between the day's high and low prices
   */
  public double getDailyRange() { return high - low; }

  /**
   * Returns the midpoint price for the day (average of high and low).
   * @return the average of the day's high and low prices
   */
  public double getMidPrice() { return (high + low) / 2.0; }

  /**
   * Returns the percent change from open to close for the day.
   * @return percent change
   */
  public double getOpenToCloseChange() {
    return (close - open) / open * 100.0;
  }

  /**
   * Returns a one-line summary of this price record.
   * Implements the abstract method from {@link StockData}.
   * @return a formatted price summary string
   */
  @Override
  public String getSummary() {
    return String.format("[Price] %s  close=%.2f  range=%.2f  vol=%d",
        getDate(), close, getDailyRange(), volume);
  }

  @Override
  public String toString() {
    return String.format(
        "PricePoint[date=%s, open=%.2f, high=%.2f, low=%.2f, close=%.2f, vol=%d]",
        getDate(), open, high, low, close, volume);
  }
}