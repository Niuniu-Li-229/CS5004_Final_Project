package stockreview.model;

public class Stock {

  private final String ticker;
  private final String companyName;

  /**
   * Constructs a Stock with the given ticker and company name.
   *
   * @param ticker      the stock ticker symbol
   * @param companyName the full company name
   * @throws IllegalArgumentException if ticker or companyName is null or blank
   */
  public Stock(String ticker, String companyName) {
    if (ticker == null || ticker.isBlank()) {
      throw new IllegalArgumentException("Ticker must not be null or blank.");
    }
    if (companyName == null || companyName.isBlank()) {
      throw new IllegalArgumentException("Company name must not be null or blank.");
    }
    this.ticker = ticker.toUpperCase();
    this.companyName = companyName;
  }

  /** @return the ticker symbol in uppercase */
  public String getTicker() { return ticker; }

  /** @return the company name */
  public String getCompanyName() { return companyName; }

  @Override
  public String toString() {
    return ticker + " (" + companyName + ")";
  }
}