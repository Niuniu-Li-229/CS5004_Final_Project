package stockreview.view;

import stockreview.controller.StockReviewController;
import stockreview.model.AnalysisResult;
import stockreview.model.StockDataModel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Main application window for the Stock Review tool.
 * Acts as the View root in the MVC pattern: it owns all sub-panels,
 * holds a reference to the controller, and routes user events between
 * the controller and the individual panels.
 *
 * <p>Layout (BorderLayout):
 * <pre>
 *   NORTH  — InputPanel  (ticker, date range, buttons)
 *   CENTER — JSplitPane(VERTICAL)
 *              top: JSplitPane(HORIZONTAL)
 *                     left:  PriceChartPanel
 *                     right: AnalysisPanel
 *              bottom: EventTablePanel
 * </pre>
 */
public class MainFrame extends JFrame {

  private final StockReviewController controller;
  private final InputPanel            inputPanel;
  private final PriceChartPanel       chartPanel;
  private final AnalysisPanel         analysisPanel;
  private final EventTablePanel       eventTablePanel;

  /**
   * Constructs the main window, wires all sub-panels together, and attempts
   * to auto-load the TSLA sample data from the project data directory.
   */
  public MainFrame() {
    super("Stock Review — CS5004 Final Project");
    this.controller = new StockReviewController();

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(1280, 820);
    setMinimumSize(new Dimension(960, 620));
    setLayout(new BorderLayout(2, 2));

    // Instantiate panels
    inputPanel      = new InputPanel(this);
    chartPanel      = new PriceChartPanel();
    analysisPanel   = new AnalysisPanel(this);
    eventTablePanel = new EventTablePanel();

    // Horizontal split: chart on the left, analysis results on the right
    JSplitPane hSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chartPanel, analysisPanel);
    hSplit.setResizeWeight(0.68);
    hSplit.setDividerLocation(860);

    // Vertical split: chart+analysis on top, event table on bottom
    JSplitPane vSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, hSplit, eventTablePanel);
    vSplit.setResizeWeight(0.62);
    vSplit.setDividerLocation(500);

    add(inputPanel, BorderLayout.NORTH);
    add(vSplit,     BorderLayout.CENTER);

    tryAutoLoad();
    setLocationRelativeTo(null);
  }

  // ── actions called by sub-panels ──────────────────────────────────────────

  /**
   * Called by InputPanel when the user clicks "Load Files".
   * Opens two sequential file-chooser dialogs (price CSV, then news CSV).
   */
  public void onLoadFiles() {
    JFileChooser fc = new JFileChooser();
    fc.setDialogTitle("Select Price CSV File");
    if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
    File priceFile = fc.getSelectedFile();

    fc.setDialogTitle("Select News / Events CSV File");
    if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
    File eventFile = fc.getSelectedFile();

    String ticker = inputPanel.getTicker().trim().toUpperCase();
    if (ticker.isEmpty()) ticker = "STOCK";

    try {
      controller.loadData(ticker, priceFile, eventFile);
      StockDataModel m = controller.getModel();
      JOptionPane.showMessageDialog(this,
          String.format("Loaded %d price points and %d events for %s.",
              m.getPriceCount(), m.getEventCount(), ticker),
          "Data Loaded", JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception ex) {
      showError("Failed to load data: " + ex.getMessage());
    }
  }

  /**
   * Called by InputPanel when the user clicks "Analyze".
   * Validates the date fields and delegates to runAnalysis().
   */
  public void onAnalyze() {
    if (!controller.isDataLoaded()) {
      showError("No data loaded. Please click \"Load Files\" first.");
      return;
    }
    try {
      LocalDate start = LocalDate.parse(inputPanel.getStartDate());
      LocalDate end   = LocalDate.parse(inputPanel.getEndDate());
      runAnalysis(start, end);
    } catch (DateTimeParseException ex) {
      showError("Invalid date format. Please use YYYY-MM-DD (e.g., 2023-01-01).");
    }
  }

  /**
   * Runs the analysis for the given period and refreshes all sub-panels.
   *
   * @param start the analysis start date (inclusive)
   * @param end   the analysis end date (inclusive)
   */
  public void runAnalysis(LocalDate start, LocalDate end) {
    try {
      AnalysisResult result = controller.analyze(start, end);
      StockDataModel model  = controller.getModel();
      chartPanel.update(model, result, start, end);
      analysisPanel.update(result);
      eventTablePanel.update(model.getEvents(), start, end);
    } catch (Exception ex) {
      showError("Analysis failed: " + ex.getMessage());
    }
  }

  /**
   * Called by AnalysisPanel when the user selects an anomaly from the list.
   * Synchronises the chart highlight and event-table scroll position.
   *
   * @param anomaly the selected anomaly, or null to clear the highlight
   */
  public void onAnomalySelected(AnalysisResult.AnomalyPoint anomaly) {
    chartPanel.highlightAnomaly(anomaly);
    if (anomaly != null) eventTablePanel.highlightDate(anomaly.getDate());
  }

  // ── private helpers ───────────────────────────────────────────────────────

  /**
   * Attempts to load TSLA sample data from the project data directory on
   * startup. Tries several candidate paths relative to the working directory
   * and the user's home folder.  Silently skips if files are not found.
   */
  private void tryAutoLoad() {
    File[] candidates = {
        new File("CS5004_Final_Project-Data"),
        new File("../CS5004_Final_Project-Data"),
        new File("../../CS5004_Final_Project-Data"),
        new File(System.getProperty("user.home"),
            "Downloads/CS5004_final/CS5004_Final_Project-Data")
    };
    for (File dir : candidates) {
      File priceFile = new File(dir, "TSLA_price.csv");
      File eventFile = new File(dir, "TSLA_news.csv");
      if (!priceFile.exists() || !eventFile.exists()) continue;
      try {
        controller.loadData("TSLA", priceFile, eventFile);
        inputPanel.setTicker("TSLA");
        LocalDate end   = LocalDate.of(2025, 7, 1);
        LocalDate start = end.minusYears(2);
        inputPanel.setDateRange(start, end);
        runAnalysis(start, end);
        return;
      } catch (Exception ignored) {
        // continue to next candidate
      }
    }
  }

  private void showError(String message) {
    JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
  }

  // ── entry point ───────────────────────────────────────────────────────────

  /**
   * Application entry point. Applies the system look-and-feel and opens
   * the main window on the Event Dispatch Thread.
   *
   * @param args command-line arguments (not used)
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception ignored) {}
      new MainFrame().setVisible(true);
    });
  }
}
