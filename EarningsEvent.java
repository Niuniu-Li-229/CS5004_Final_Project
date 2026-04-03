import java.time.LocalDate;

/**
 * A specialized {@link MarketEvent} that carries earnings-specific data:
 * the reported EPS and whether the company beat analyst expectations.
 *
 */
public class EarningsEvent extends MarketEvent {

  private final double  reportedEps;
  private final boolean beatExpectations;

  /**
   * Constructs an EarningsEvent with earnings-specific fields.
   *
   * @param date             the earnings release date, must not be null
   * @param title            the headline, must not be blank
   * @param description      a brief description, must not be null
   * @param source           the publication name, must not be blank
   * @param reportedEps      the reported earnings per share
   * @param beatExpectations true if the company beat analyst EPS estimates
   */
  public EarningsEvent(LocalDate date, String title, String description,
      String source, double reportedEps, boolean beatExpectations) {
    super(date, title, description, source, EventType.EARNINGS);
    this.reportedEps      = reportedEps;
    this.beatExpectations = beatExpectations;
  }

  /** @return the reported earnings per share */
  public double getReportedEps()    { return reportedEps; }

  /** @return true if reported EPS beat analyst consensus estimates */
  public boolean beatExpectations() { return beatExpectations; }

  /**
   * Overrides the parent summary to include EPS and beat/miss status.
   * @return a formatted earnings summary string
   */
  @Override
  public String getSummary() {
    String beat = beatExpectations ? "BEAT" : "MISSED";
    return String.format("[EARNINGS] %s — EPS: %.2f (%s) (%s)",
        getTitle(), reportedEps, beat, getDate());
  }
}
