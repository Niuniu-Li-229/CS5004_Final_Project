package stockreview.controller;

import stockreview.model.AnalysisResult;
import stockreview.model.MarketEvent;
import stockreview.model.PricePoint;
import stockreview.model.StockDataModel;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * MVC Controller for the Stock Review application. Coordinates data loading
 * via CsvLoader and anomaly analysis via StockAnalyzer, and exposes the
 * resulting model and results to the View layer. Maintains the current
 * StockDataModel and the most recent AnalysisResult as mutable state.
 */
public class StockReviewController {

  private StockDataModel model;
  private AnalysisResult lastResult;

  /**
   * Loads price and event data from CSV files for the given ticker symbol.
   * Replaces any previously loaded data. After a successful call, isDataLoaded()
   * returns true and analyze() may be called.
   *
   * @param ticker     the stock ticker symbol (e.g., "TSLA"), must not be blank
   * @param priceFile  CSV file containing OHLCV price data, must not be null
   * @param eventFile  CSV file containing news event data, must not be null
   * @throws IOException              if either CSV file cannot be read
   * @throws IllegalArgumentException if any argument is null or ticker is blank
   */
  public void loadData(String ticker, File priceFile, File eventFile) throws IOException {
    if (ticker == null || ticker.isBlank())
      throw new IllegalArgumentException("Ticker must not be blank.");
    if (priceFile == null || eventFile == null)
      throw new IllegalArgumentException("CSV files must not be null.");

    List<PricePoint>  prices = CsvLoader.loadPrices(priceFile);
    List<MarketEvent> events = CsvLoader.loadEvents(eventFile);
    this.model      = new StockDataModel(ticker, prices, events);
    this.lastResult = null;
  }

  /**
   * Runs anomaly detection on the loaded model for the given date range
   * and stores the result. Requires data to have been loaded first.
   *
   * @param start start date (inclusive), must not be null
   * @param end   end date (inclusive), must not be null
   * @return the AnalysisResult for the requested period
   * @throws IllegalStateException    if no data has been loaded yet
   * @throws IllegalArgumentException if dates are null or start is after end
   */
  public AnalysisResult analyze(LocalDate start, LocalDate end) {
    if (model == null)
      throw new IllegalStateException("No data loaded. Call loadData() first.");
    this.lastResult = StockAnalyzer.analyze(model, start, end);
    return lastResult;
  }

  /**
   * Returns the currently loaded StockDataModel.
   *
   * @return the model, or null if no data has been loaded
   */
  public StockDataModel getModel() { return model; }

  /**
   * Returns the most recent AnalysisResult produced by analyze().
   *
   * @return the last result, or null if analyze() has not been called
   */
  public AnalysisResult getLastResult() { return lastResult; }

  /**
   * Returns true if price and event data have been loaded via loadData().
   *
   * @return true when a StockDataModel is available
   */
  public boolean isDataLoaded() { return model != null; }
}
