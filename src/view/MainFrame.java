package view;

import model.*;
import controller.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * The main application window for the TSLA Stock Analyzer. All three panels
 * ({@link PriceChartPanel}, {@link EventTablePanel}, {@link AnalysisPanel})
 * are visible simultaneously on a single screen divided by resizable split
 * panes — no tabs or sub-pages. The left column shows the price chart above
 * and the analysis result below; the right column shows all news events with
 * filtering and a detail view. A control bar at the top drives data loading
 * and analysis. Data I/O runs on a background {@link SwingWorker} thread.
 */
public class MainFrame extends JFrame {

  // Default CSV paths (relative to the working directory)
  private static final String DEFAULT_PRICE_CSV = "data/TSLA_price.csv";
  private static final String DEFAULT_NEWS_CSV  = "data/TSLA_news.csv";

  // Default date range shown in the control bar on start-up
  private static final String DEFAULT_START = "2022-01-01";
  private static final String DEFAULT_END   = "2024-12-31";

  private final StockController  controller;
  private final PriceChartPanel  chartPanel;
  private final EventTablePanel  eventPanel;
  private final AnalysisPanel    analysisPanel;

  private final JTextField startField;
  private final JTextField endField;
  private final JSpinner   thresholdSpinner;
  private final JButton    analyzeButton;
  private final JLabel     statusBar;


  /**
   * Constructs and displays the main application window.
   * Attempts to auto-load the default CSV files if they exist in the
   * current working directory; otherwise prompts the user to locate them.
   */
  public MainFrame() {
    super("Stock Analyzer — CS 5004 Final Project");
    this.controller = new StockController();

    // ---- View panels -------------------------------------------------------
    chartPanel    = new PriceChartPanel();
    eventPanel    = new EventTablePanel();
    analysisPanel = new AnalysisPanel();

    // ---- Control bar -------------------------------------------------------
    startField = new JTextField(DEFAULT_START, 10);
    endField   = new JTextField(DEFAULT_END,   10);

    SpinnerNumberModel spinModel =
        new SpinnerNumberModel(StockController.DEFAULT_THRESHOLD, 1.0, 30.0, 0.5);
    thresholdSpinner = new JSpinner(spinModel);
    ((JSpinner.NumberEditor) thresholdSpinner.getEditor()).getFormat().setMinimumFractionDigits(1);

    analyzeButton = new JButton("Run Analysis");
    analyzeButton.addActionListener(this::onAnalyzeClicked);

    JButton loadButton = new JButton("Load CSV…");
    loadButton.addActionListener(this::onLoadClicked);

    JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    controlBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
    controlBar.add(new JLabel("From:"));
    controlBar.add(startField);
    controlBar.add(new JLabel("To:"));
    controlBar.add(endField);
    controlBar.add(Box.createHorizontalStrut(10));
    controlBar.add(new JLabel("Anomaly threshold (%):"));
    controlBar.add(thresholdSpinner);
    controlBar.add(Box.createHorizontalStrut(10));
    controlBar.add(analyzeButton);
    controlBar.add(loadButton);

    // ---- Wrap chart in a titled panel so the section is clearly labelled ----
    JPanel chartWrapper = new JPanel(new BorderLayout());
    chartWrapper.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), "Price Chart",
        javax.swing.border.TitledBorder.LEFT,
        javax.swing.border.TitledBorder.TOP,
        new Font("SansSerif", Font.BOLD, 12)));
    chartWrapper.add(chartPanel, BorderLayout.CENTER);

    // ---- Left column: chart (top) + analysis (bottom) ----------------------
    JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        chartWrapper, analysisPanel);
    leftSplit.setResizeWeight(0.50);
    leftSplit.setDividerSize(6);
    leftSplit.setBorder(null);

    // ---- Main split: left column (55%) + news events (45%) ----------------
    JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
        leftSplit, eventPanel);
    mainSplit.setResizeWeight(0.55);
    mainSplit.setDividerSize(6);
    mainSplit.setBorder(null);

    // ---- Status bar --------------------------------------------------------
    statusBar = new JLabel(" Ready. Use 'Load CSV…' to choose custom files, "
        + "or click 'Run Analysis' to use defaults.");
    statusBar.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
    statusBar.setFont(new Font("SansSerif", Font.PLAIN, 11));
    statusBar.setForeground(Color.DARK_GRAY);

    // ---- Layout ------------------------------------------------------------
    setLayout(new BorderLayout());
    add(controlBar, BorderLayout.NORTH);
    add(mainSplit,  BorderLayout.CENTER);
    add(statusBar,  BorderLayout.SOUTH);

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(1280, 800);
    setLocationRelativeTo(null);
    setVisible(true);

    // Auto-load if default files exist
    if (new File(DEFAULT_PRICE_CSV).exists() && new File(DEFAULT_NEWS_CSV).exists()) {
      loadDataInBackground(DEFAULT_PRICE_CSV, DEFAULT_NEWS_CSV);
    }
  }

  // -------------------------------------------------------------------------
  // Action handlers
  // -------------------------------------------------------------------------

  /**
   * Handles the Run Analysis button click. Delegates to {@link #runAnalysis()}.
   *
   * @param e the action event (unused beyond triggering this handler)
   */
  private void onAnalyzeClicked(ActionEvent e) {
    runAnalysis();
  }

  /**
   * Validates the current date fields and threshold, then runs the analysis
   * on a background thread and updates all three panels when complete.
   * Called automatically after data loads and whenever the user clicks the button.
   */
  private void runAnalysis() {
    if (!controller.isDataLoaded()) {
      showError("No data loaded",
          "Please click 'Load CSV…' to select a price CSV and a news CSV before running analysis.");
      return;
    }

    LocalDate start, end;
    try {
      start = LocalDate.parse(startField.getText().trim());
    } catch (DateTimeParseException ex) {
      highlightField(startField, true);
      showError("Invalid start date",
          "'" + startField.getText().trim() + "' is not a valid date.\nExpected format: YYYY-MM-DD  (e.g. 2022-01-01)");
      return;
    }
    try {
      end = LocalDate.parse(endField.getText().trim());
    } catch (DateTimeParseException ex) {
      highlightField(endField, true);
      showError("Invalid end date",
          "'" + endField.getText().trim() + "' is not a valid date.\nExpected format: YYYY-MM-DD  (e.g. 2024-12-31)");
      return;
    }
    if (end.isBefore(start)) {
      highlightField(startField, true);
      highlightField(endField, true);
      showError("Invalid date range",
          "Start date (" + start + ") must not be later than end date (" + end + ").\n"
          + "Please swap the two dates or choose a valid range.");
      return;
    }

    // Clear any previous field highlights on valid input
    highlightField(startField, false);
    highlightField(endField, false);

    double threshold = ((Number) thresholdSpinner.getValue()).doubleValue();
    setStatus("Running analysis…", false);
    analyzeButton.setEnabled(false);

    SwingWorker<AnalysisResult, Void> worker = new SwingWorker<>() {
      @Override
      protected AnalysisResult doInBackground() {
        return controller.analyze(start, end, threshold);
      }

      @Override
      protected void done() {
        try {
          AnalysisResult result = get();
          StockDataModel model  = controller.getModel();

          chartPanel.setData(
              model.getPricesInRange(start, end),
              result.getAnomalies());
          analysisPanel.setResult(result);

          setStatus(result.getSummary(), false);
        } catch (Exception ex) {
          MainFrame.this.showError("Analysis failed", ex.getMessage());
          setStatus("Analysis failed — see error dialog.", true);
        } finally {
          analyzeButton.setEnabled(true);
        }
      }
    };
    worker.execute();
  }

  /**
   * Handles the Load CSV… button click. Opens two file chooser dialogs so
   * the user can select a price CSV and a news CSV, then triggers a background
   * data load. Falls back to the default paths if the user cancels.
   *
   * @param e the action event (unused beyond triggering this handler)
   */
  private void onLoadClicked(ActionEvent e) {
    JFileChooser fc = new JFileChooser(new File("."));
    fc.setDialogTitle("Select Price CSV  (e.g. AAPL_price.csv)");
    if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
    String price = fc.getSelectedFile().getAbsolutePath();

    fc.setDialogTitle("Select News CSV  (e.g. AAPL_news.csv)");
    if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
    String news = fc.getSelectedFile().getAbsolutePath();

    loadDataInBackground(price, news);
  }

  // -------------------------------------------------------------------------
  // Background loading
  // -------------------------------------------------------------------------

  /**
   * Loads price and news CSV files on a background thread so the UI stays
   * responsive. On success, populates the EventTablePanel with all events.
   *
   * @param priceCsv absolute or relative path to the price CSV
   * @param newsCsv  absolute or relative path to the news CSV
   */
  private void loadDataInBackground(String priceCsv, String newsCsv) {
    analyzeButton.setEnabled(false);
    setStatus("Loading CSV data…", false);

    SwingWorker<Void, Void> worker = new SwingWorker<>() {
      @Override
      protected Void doInBackground() throws Exception {
        controller.loadData(priceCsv, newsCsv);
        return null;
      }

      @Override
      protected void done() {
        try {
          get(); // re-throws any exception from doInBackground
          StockDataModel model = controller.getModel();
          eventPanel.setEvents(model.getEvents());
          setStatus(String.format(
              "Loaded %d price records and %d news events.",
              model.getPriceCount(), model.getEventCount()), false);
          runAnalysis();
        } catch (Exception ex) {
          showError("Failed to load data", ex.getMessage());
          setStatus("Data load failed — see error dialog.", true);
        } finally {
          analyzeButton.setEnabled(true);
        }
      }
    };
    worker.execute();
  }

  // -------------------------------------------------------------------------
  // Utility
  // -------------------------------------------------------------------------

  /**
   * Updates the status bar text and optionally colours it red to indicate errors.
   *
   * @param message the status message to display
   * @param isError true to show the message in red, false for normal colour
   */
  private void setStatus(String message, boolean isError) {
    statusBar.setText(" " + message);
    statusBar.setForeground(isError ? new Color(180, 40, 40) : Color.DARK_GRAY);
  }

  /**
   * Shows a modal error dialog with a title and descriptive message.
   * Used for all user-facing validation and runtime failures so they are
   * impossible to miss, unlike a small status bar change.
   *
   * @param title   short dialog title shown in the window decoration
   * @param message the full error description shown in the dialog body
   */
  private void showError(String title, String message) {
    JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Highlights a text field with a red border when its value is invalid,
   * or restores the default border when the value is corrected.
   *
   * @param field   the text field to decorate
   * @param invalid true to apply a red error border, false to clear it
   */
  private void highlightField(JTextField field, boolean invalid) {
    field.setBorder(invalid
        ? BorderFactory.createLineBorder(new Color(200, 50, 50), 2)
        : new JTextField().getBorder());
  }

  // -------------------------------------------------------------------------
  // Entry point
  // -------------------------------------------------------------------------

  /**
   * Application entry point. Starts the GUI on the Swing event dispatch thread.
   * Run from the project directory so the default CSV paths resolve correctly.
   *
   * @param args command-line arguments (not used)
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(MainFrame::new);
  }
}
