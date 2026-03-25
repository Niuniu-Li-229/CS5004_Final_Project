package stockreview.model;

import java.time.LocalDate;


public class NewsEvent {

  private final LocalDate date;
  private final String title;
  private final String description;
  private final String source;
  private final EventType type;

  /**
   * Constructs a NewsEvent with the given fields.
   *
   * @param date        the date the event occurred or was published, must not be null
   * @param title       the headline or title of the event, must not be blank
   * @param description a brief description of the event, may be empty but not null
   * @param source      the publication or data source name, must not be blank
   * @param type        the category of the event, must not be null
   * @throws IllegalArgumentException if any required field is null or blank
   */
  public NewsEvent(LocalDate date, String title, String description,
      String source, EventType type) {
    if (date == null)   throw new IllegalArgumentException("Date must not be null.");
    if (title == null || title.isBlank())
      throw new IllegalArgumentException("Title must not be blank.");
    if (description == null)
      throw new IllegalArgumentException("Description must not be null.");
    if (source == null || source.isBlank())
      throw new IllegalArgumentException("Source must not be blank.");
    if (type == null)   throw new IllegalArgumentException("EventType must not be null.");

    this.date        = date;
    this.title       = title;
    this.description = description;
    this.source      = source;
    this.type        = type;
  }

  /** @return the event date */
  public LocalDate getDate()        { return date; }

  /** @return the event headline */
  public String getTitle()          { return title; }

  /** @return the event description */
  public String getDescription()    { return description; }

  /** @return the data source or publication name */
  public String getSource()         { return source; }

  /** @return the category of the event */
  public EventType getType()        { return type; }

  @Override
  public String toString() {
    return String.format("[%s][%s] %s (%s)", date, type, title, source);
  }
}