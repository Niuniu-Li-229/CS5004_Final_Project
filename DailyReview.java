package stockreview.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class DailyReview {

  private final LocalDate date;
  private final double percentChange;
  private final boolean significant;
  private final List<NewsEvent> relatedEvents;
  private final String generatedComment;

  /**
   * Constructs a DailyReview for one trading day.
   *
   * @param date             the trading date, must not be null
   * @param percentChange    daily percent change relative to prior close
   * @param significant      true if the move exceeds the significance threshold
   * @param relatedEvents    news events near this date
   * @param generatedComment the rule-generated explanation string, must not be null
   * @throws IllegalArgumentException if date, relatedEvents, or generatedComment is null
   */
  public DailyReview(LocalDate date, double percentChange, boolean significant,
      List<NewsEvent> relatedEvents, String generatedComment) {
    if (date == null)           throw new IllegalArgumentException("Date must not be null.");
    if (relatedEvents == null)  throw new IllegalArgumentException("relatedEvents must not be null.");
    if (generatedComment == null)
      throw new IllegalArgumentException("generatedComment must not be null.");

    this.date             = date;
    this.percentChange    = percentChange;
    this.significant      = significant;
    this.relatedEvents    = Collections.unmodifiableList(new ArrayList<>(relatedEvents));
    this.generatedComment = generatedComment;
  }

  /** @return the trading date */
  public LocalDate getDate()             { return date; }

  /** @return percent change from prior close */
  public double getPercentChange()       { return percentChange; }

  /** @return true if this day's move exceeded the significance threshold */
  public boolean isSignificant()         { return significant; }

  /**
   *
   * @return list of related {@link NewsEvent} objects
   */
  public List<NewsEvent> getRelatedEvents() { return relatedEvents; }

  /** @return the rule-generated commentary for this day */
  public String getGeneratedComment()    { return generatedComment; }

  @Override
  public String toString() {
    return String.format("DailyReview[date=%s, change=%.2f%%, significant=%b, events=%d]",
        date, percentChange, significant, relatedEvents.size());
  }
}