import java.time.LocalDate;

/**
 * Represents a news article or market event associated with a stock.
 */
public class MarketEvent extends StockData {

  private final String    title;
  private final String    description;
  private final String    source;
  private final EventType type;

  /**
   * Constructs a MarketEvent with the given fields.
   * @param date        the publication date, must not be null
   * @param title       the headline, must not be blank
   * @param description a brief description, must not be null
   * @param source      the publication name, must not be blank
   * @param type        the event category, must not be null
   * @throws IllegalArgumentException if any required field is null or blank
   */
  public MarketEvent(LocalDate date, String title, String description,
      String source, EventType type) {
    super(date);
    requireNonBlank(title,       "Title");
    requireNonNull(description,  "Description");
    requireNonBlank(source,      "Source");
    requireNonNull(type,         "EventType");

    this.title       = title;
    this.description = description;
    this.source      = source;
    this.type        = type;
  }

  /** @return the event headline */
  public String getTitle()       { return title; }

  /** @return the event description */
  public String getDescription() { return description; }

  /** @return the data source or publication name */
  public String getSource()      { return source; }

  /** @return the category of the event */
  public EventType getType()     { return type; }

  /**
   * Returns true if this event matches the given type.
   * @param eventType the type to check against
   * @return true if this event's type equals the given type
   */
  public boolean isType(EventType eventType) { return this.type == eventType; }

  /**
   * Returns a one-line summary of this market event.
   * Implements the abstract method from {@link StockData}.
   * @return a formatted event summary string
   */
  @Override
  public String getSummary() {
    return String.format("[%s] %s — %s (%s)", type, title, source, getDate());
  }

  @Override
  public String toString() {
    return String.format("[%s][%s] %s (%s)", getDate(), type, title, source);
  }
}
