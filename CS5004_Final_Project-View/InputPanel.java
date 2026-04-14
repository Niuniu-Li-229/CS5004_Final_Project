package stockreview.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;

/**
 * The top control bar of the Stock Review application.
 * Provides a ticker symbol field, start/end date text fields (YYYY-MM-DD),
 * a "Load Files" button that opens CSV file-chooser dialogs, and an
 * "Analyze" button that triggers anomaly analysis for the entered period.
 */
public class InputPanel extends JPanel {

  private final MainFrame  mainFrame;
  private final JTextField tickerField;
  private final JTextField startField;
  private final JTextField endField;

  /**
   * Constructs the InputPanel and wires action listeners to the given MainFrame.
   *
   * @param mainFrame the parent main window that handles button actions
   */
  public InputPanel(MainFrame mainFrame) {
    this.mainFrame = mainFrame;

    setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));
    setBorder(new EmptyBorder(4, 8, 4, 8));
    setBackground(new Color(45, 45, 48));

    // Ticker symbol
    add(label("Ticker:"));
    tickerField = textField("TSLA", 6);
    add(tickerField);

    addSeparator();

    // Date range
    add(label("From:"));
    startField = textField(LocalDate.now().minusYears(2).toString(), 11);
    add(startField);

    add(label("To:"));
    endField = textField(LocalDate.now().toString(), 11);
    add(endField);

    addSeparator();

    // Action buttons
    JButton loadBtn    = button("Load Files", new Color(0, 122, 204));
    JButton analyzeBtn = button("Analyze",    new Color(40, 167, 69));

    loadBtn.addActionListener(e -> mainFrame.onLoadFiles());
    analyzeBtn.addActionListener(e -> mainFrame.onAnalyze());

    add(loadBtn);
    add(analyzeBtn);
  }

  // ── public accessors ──────────────────────────────────────────────────────

  /** @return the text currently in the ticker field (may be empty) */
  public String getTicker()    { return tickerField.getText(); }

  /** @return the text currently in the start-date field */
  public String getStartDate() { return startField.getText(); }

  /** @return the text currently in the end-date field */
  public String getEndDate()   { return endField.getText(); }

  /**
   * Sets the ticker symbol displayed in the input field.
   * @param ticker the ticker symbol to display
   */
  public void setTicker(String ticker) {
    tickerField.setText(ticker);
  }

  /**
   * Sets both date fields to the given start and end dates.
   *
   * @param start the start date to display
   * @param end   the end date to display
   */
  public void setDateRange(LocalDate start, LocalDate end) {
    startField.setText(start.toString());
    endField.setText(end.toString());
  }

  // ── private factory helpers ───────────────────────────────────────────────

  private JLabel label(String text) {
    JLabel l = new JLabel(text);
    l.setForeground(Color.WHITE);
    l.setFont(new Font("SansSerif", Font.BOLD, 13));
    return l;
  }

  private JTextField textField(String defaultText, int cols) {
    JTextField f = new JTextField(defaultText, cols);
    f.setFont(new Font("Monospaced", Font.PLAIN, 13));
    return f;
  }

  private JButton button(String text, Color bg) {
    JButton b = new JButton(text);
    b.setBackground(bg);
    b.setForeground(Color.WHITE);
    b.setFont(new Font("SansSerif", Font.BOLD, 13));
    b.setFocusPainted(false);
    b.setOpaque(true);
    b.setBorderPainted(false);
    b.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
    return b;
  }

  /** Adds a small vertical separator line between groups of controls. */
  private void addSeparator() {
    JSeparator sep = new JSeparator(JSeparator.VERTICAL);
    sep.setPreferredSize(new Dimension(1, 24));
    sep.setForeground(new Color(100, 100, 100));
    add(sep);
  }
}
