
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the complete result of analyzing a stock over a date range.
 * Contains a list of {@link AnomalyPoint} objects, each representing
 * a significant price move and its related news events.
 */
public class AnalysisResult {

  private final String           ticker;
  private final LocalDate        startDate;
  private final LocalDate        endDate;
  private final double           totalReturn;
  private final List<AnomalyPoint> anomalies;
  private final String           summary;

  /**
   * Constructs an AnalysisResult with the given fields.
   *
   * @param ticker      the stock ticker symbol, must not be blank
   * @param startDate   the start of the review period, must not be null
   * @param endDate     the end of the review period, must not be null
   * @param totalReturn total percent return over the period
   * @param anomalies   list of anomaly points found, must not be null
   * @param summary     a plain-text summary of the analysis, must not be null
   * @throws IllegalArgumentException if any required field is null or blank
   */
  public AnalysisResult(String ticker, LocalDate startDate, LocalDate endDate,
      double totalReturn, List<AnomalyPoint> anomalies, String summary) {
    if (ticker == null || ticker.isBlank())
      throw new IllegalArgumentException("Ticker must not be blank.");
    if (startDate == null || endDate == null)
      throw new IllegalArgumentException("Dates must not be null.");
    if (endDate.isBefore(startDate))
      throw new IllegalArgumentException("End date must not be before start date.");
    if (anomalies == null)
      throw new IllegalArgumentException("Anomalies list must not be null.");
    if (summary == null)
      throw new IllegalArgumentException("Summary must not be null.");

    this.ticker      = ticker.toUpperCase();
    this.startDate   = startDate;
    this.endDate     = endDate;
    this.totalReturn = totalReturn;
    this.anomalies   = Collections.unmodifiableList(new ArrayList<>(anomalies));
    this.summary     = summary;
  }

  /** @return the stock ticker symbol */
  public String getTicker()                  { return ticker; }

  /** @return the start date of the review period */
  public LocalDate getStartDate()            { return startDate; }

  /** @return the end date of the review period */
  public LocalDate getEndDate()              { return endDate; }

  /** @return total percent return over the period */
  public double getTotalReturn()             { return totalReturn; }

  /**
   * Returns an unmodifiable view of all anomaly points found.
   *
   * @return list of {@link AnomalyPoint} objects
   */
  public List<AnomalyPoint> getAnomalies()   { return anomalies; }

  /** @return the plain-text summary of the analysis */
  public String getSummary()                 { return summary; }

  /** @return the number of anomaly points found */
  public int getAnomalyCount()               { return anomalies.size(); }

  @Override
  public String toString() {
    return String.format("AnalysisResult[ticker=%s, period=%s→%s, return=%.2f%%, anomalies=%d]",
        ticker, startDate, endDate, totalReturn, anomalies.size());
  }


  /**
   * Represents a single anomalous trading day: a day where the price move
   * exceeded the significance threshold. Holds the price record for that day,
   * any nearby news events, and a generated explanation comment.
   */
  public static class AnomalyPoint {

    private final PricePoint        pricePoint;
    private final double            percentChange;
    private final List<MarketEvent> relatedEvents;
    private final String            comment;

    /**
     * Constructs an AnomalyPoint for one anomalous trading day.
     *
     * @param pricePoint    the price record for this day, must not be null
     * @param percentChange the daily percent change that triggered the anomaly
     * @param relatedEvents nearby news events; a defensive copy is made
     * @param comment       the generated explanation, must not be null
     * @throws IllegalArgumentException if pricePoint, relatedEvents, or comment is null
     */
    public AnomalyPoint(PricePoint pricePoint, double percentChange,
        List<MarketEvent> relatedEvents, String comment) {
      if (pricePoint == null)
        throw new IllegalArgumentException("PricePoint must not be null.");
      if (relatedEvents == null)
        throw new IllegalArgumentException("relatedEvents must not be null.");
      if (comment == null)
        throw new IllegalArgumentException("Comment must not be null.");

      this.pricePoint    = pricePoint;
      this.percentChange = percentChange;
      this.relatedEvents = Collections.unmodifiableList(new ArrayList<>(relatedEvents));
      this.comment       = comment;
    }

    /** @return the price record for this anomalous day */
    public PricePoint getPricePoint()          { return pricePoint; }

    /** @return the daily percent change */
    public double getPercentChange()           { return percentChange; }

    /**
     * Returns an unmodifiable view of related news events near this date.
     * @return list of {@link MarketEvent} objects
     */
    public List<MarketEvent> getRelatedEvents() { return relatedEvents; }

    /** @return the generated explanation comment */
    public String getComment()                 { return comment; }

    /** @return the date of this anomalous day */
    public LocalDate getDate()                 { return pricePoint.getDate(); }

    /** @return true if the price moved up on this day */
    public boolean isGain()                    { return percentChange > 0; }

    @Override
    public String toString() {
      return String.format("AnomalyPoint[date=%s, change=%+.2f%%, events=%d]",
          getDate(), percentChange, relatedEvents.size());
    }
  }
}
