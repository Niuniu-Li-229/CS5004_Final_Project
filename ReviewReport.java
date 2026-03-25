package stockreview.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ReviewReport {

  private final String ticker;
  private final LocalDate startDate;
  private final LocalDate endDate;
  private final double totalReturn;
  private final PriceRecord biggestGainDay;
  private final PriceRecord biggestLossDay;
  private final List<DailyReview> dailyReviews;
  private final String summary;

  /**
   * Constructs a ReviewReport
   *
   * @param ticker         the stock ticker symbol
   * @param startDate      the start of the review period, must not be null
   * @param endDate        the end of the review period, must be on or after startDate
   * @param totalReturn    total percent return over the period
   * @param biggestGainDay the trading day with the highest single-day gain; may be null
   *                       if fewer than two price records exist
   * @param biggestLossDay the trading day with the largest single-day loss; may be null
   *                       if fewer than two price records exist
   * @param dailyReviews   per-day analysis objects; a defensive copy is made
   * @param summary        the textual summary of the review period, must not be null
   * @throws IllegalArgumentException if required fields are null or dates are invalid
   */
  public ReviewReport(String ticker, LocalDate startDate, LocalDate endDate,
      double totalReturn, PriceRecord biggestGainDay,
      PriceRecord biggestLossDay, List<DailyReview> dailyReviews,
      String summary) {
    if (ticker == null || ticker.isBlank())
      throw new IllegalArgumentException("Ticker must not be blank.");
    if (startDate == null || endDate == null)
      throw new IllegalArgumentException("Start and end dates must not be null.");
    if (endDate.isBefore(startDate))
      throw new IllegalArgumentException("End date must not be before start date.");
    if (dailyReviews == null)
      throw new IllegalArgumentException("dailyReviews must not be null.");
    if (summary == null)
      throw new IllegalArgumentException("Summary must not be null.");

    this.ticker         = ticker;
    this.startDate      = startDate;
    this.endDate        = endDate;
    this.totalReturn    = totalReturn;
    this.biggestGainDay = biggestGainDay;
    this.biggestLossDay = biggestLossDay;
    this.dailyReviews   = Collections.unmodifiableList(new ArrayList<>(dailyReviews));
    this.summary        = summary;
  }

  /** @return the ticker symbol */
  public String getTicker()                    { return ticker; }

  /** @return the start date of the review period */
  public LocalDate getStartDate()              { return startDate; }

  /** @return the end date of the review period */
  public LocalDate getEndDate()                { return endDate; }

  /** @return total percent return over the period */
  public double getTotalReturn()               { return totalReturn; }

  /** @return the {@link PriceRecord} with the highest single-day gain, or null */
  public PriceRecord getBiggestGainDay()       { return biggestGainDay; }

  /** @return the {@link PriceRecord} with the largest single-day loss, or null */
  public PriceRecord getBiggestLossDay()       { return biggestLossDay; }

  /**
   *
   * @return list of {@link DailyReview} objects ordered by date
   */
  public List<DailyReview> getDailyReviews()   { return dailyReviews; }

  /** @return the textual summary of the review period */
  public String getSummary()                   { return summary; }
}