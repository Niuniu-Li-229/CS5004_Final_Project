package stockreview.model;

import java.time.LocalDate;

public class PriceRecord {

  private final LocalDate date;
  private final double open;
  private final double high;
  private final double low;
  private final double close;
  private final long volume;

  /**
   * Constructs a PriceRecord for a stock
   *
   * @param date   the trading date, must not be null
   * @param open   opening price, must be positive
   * @param high   highest price of the day, must be >= open and >= close
   * @param low    lowest price of the day, must be > 0 and <= open and <= close
   * @param close  closing price, must be positive
   * @param volume trading volume, must be non-negative
   * @throws IllegalArgumentException if any value is invalid
   */
  public PriceRecord(LocalDate date, double open, double high,
      double low, double close, long volume) {
    if (date == null) throw new IllegalArgumentException("Date must not be null.");
    if (open <= 0 || high <= 0 || low <= 0 || close <= 0) {
      throw new IllegalArgumentException("Price values must be positive.");
    }
    if (high < low) throw new IllegalArgumentException("High must be >= low.");
    if (volume < 0)  throw new IllegalArgumentException("Volume must be non-negative.");

    this.date   = date;
    this.open   = open;
    this.high   = high;
    this.low    = low;
    this.close  = close;
    this.volume = volume;
  }

  /** @return the trading date */
  public LocalDate getDate()  { return date; }

  /** @return the opening price */
  public double getOpen()     { return open; }

  /** @return the highest price of the day */
  public double getHigh()     { return high; }

  /** @return the lowest price of the day */
  public double getLow()      { return low; }

  /** @return the closing price */
  public double getClose()    { return close; }

  /** @return the trading volume */
  public long getVolume()     { return volume; }

  @Override
  public String toString() {
    return String.format("PriceRecord[date=%s, open=%.2f, high=%.2f, low=%.2f, close=%.2f, vol=%d]",
        date, open, high, low, close, volume);
  }
}