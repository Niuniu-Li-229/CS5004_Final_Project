package model;

import java.time.LocalDate;

/**
 * Represents Abstract base class for all time-stamped stock data objects.
 */
public abstract class StockData implements Summarizable {

  private final LocalDate date;

  /**
   * Constructs a StockData object with the given date.
   * @param date the date this data point belongs to, must not be null
   */
  protected StockData(LocalDate date) {
    requireNonNull(date, "Date");
    this.date = date;
  }

  /**
   * Returns the date associated with this data point.
   * @return the trading or publication date
   */
  public LocalDate getDate() { return date; }

  /**
   * Returns a short human-readable summary of this data point.
   * @return a non-null summary string
   */
  public abstract String getSummary();

  protected static void requireNonNull(Object value, String fieldName) {
    if (value == null)
      throw new IllegalArgumentException(fieldName + " must not be null.");
  }

  /**
   * Throws {@link IllegalArgumentException} if the given string is null or blank.
   */
  protected static void requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException(fieldName + " must not be blank.");
  }

  /**
   * Throws {@link IllegalArgumentException} if the given double is not positive.
   */
  protected static void requirePositive(double value, String fieldName) {
    if (value <= 0)
      throw new IllegalArgumentException(fieldName + " must be positive.");
  }
}
