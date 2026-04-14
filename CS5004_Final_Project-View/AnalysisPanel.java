package stockreview.view;

import stockreview.model.AnalysisResult;
import stockreview.model.AnalysisResult.AnomalyPoint;
import stockreview.model.MarketEvent;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Right-side panel that displays analysis results for the selected period.
 * Shows the total return, anomaly count, a selectable list of anomalous
 * trading days, and a detail view that appears when the user selects one.
 *
 * <p>Selecting an anomaly from the list notifies MainFrame, which in turn
 * highlights the corresponding day on the PriceChartPanel and scrolls the
 * EventTablePanel to the nearby events.
 */
public class AnalysisPanel extends JPanel {

  private final MainFrame             mainFrame;
  private final JLabel                returnLabel;
  private final JLabel                countLabel;
  private final JTextArea             summaryArea;
  private final DefaultListModel<String> listModel;
  private final JList<String>         anomalyList;
  private final JTextArea             detailArea;

  private List<AnomalyPoint> anomalies;

  /**
   * Constructs the AnalysisPanel connected to the given MainFrame.
   *
   * @param mainFrame the parent window used for anomaly-selection callbacks
   */
  public AnalysisPanel(MainFrame mainFrame) {
    this.mainFrame = mainFrame;
    setLayout(new BorderLayout(4, 4));
    setBorder(new CompoundBorder(
        BorderFactory.createLineBorder(new Color(200, 200, 200)),
        new EmptyBorder(8, 8, 8, 8)));
    setPreferredSize(new Dimension(340, 500));

    // ── header ──
    JLabel header = new JLabel("Analysis Results", SwingConstants.CENTER);
    header.setFont(new Font("SansSerif", Font.BOLD, 14));
    header.setBorder(new EmptyBorder(0, 0, 6, 0));
    add(header, BorderLayout.NORTH);

    // ── stats row ──
    returnLabel = new JLabel("Total Return: —");
    returnLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

    countLabel = new JLabel("Anomalies: —");
    countLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

    summaryArea = new JTextArea(2, 20);
    summaryArea.setFont(new Font("SansSerif", Font.PLAIN, 11));
    summaryArea.setEditable(false);
    summaryArea.setLineWrap(true);
    summaryArea.setWrapStyleWord(true);
    summaryArea.setBackground(new Color(245, 245, 245));
    summaryArea.setBorder(new EmptyBorder(3, 4, 3, 4));

    JPanel statsPanel = new JPanel();
    statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
    statsPanel.setOpaque(false);
    statsPanel.add(returnLabel);
    statsPanel.add(Box.createVerticalStrut(3));
    statsPanel.add(countLabel);
    statsPanel.add(Box.createVerticalStrut(4));
    statsPanel.add(summaryArea);

    // ── anomaly list ──
    listModel   = new DefaultListModel<>();
    anomalyList = new JList<>(listModel);
    anomalyList.setFont(new Font("Monospaced", Font.PLAIN, 12));
    anomalyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    anomalyList.setCellRenderer(new AnomalyListRenderer());
    anomalyList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) onAnomalySelected(anomalyList.getSelectedIndex());
    });

    JScrollPane listScroll = new JScrollPane(anomalyList);
    listScroll.setBorder(titled("Anomaly Days"));

    // ── detail area ──
    detailArea = new JTextArea(7, 20);
    detailArea.setFont(new Font("SansSerif", Font.PLAIN, 11));
    detailArea.setEditable(false);
    detailArea.setLineWrap(true);
    detailArea.setWrapStyleWord(true);
    JScrollPane detailScroll = new JScrollPane(detailArea);
    detailScroll.setBorder(titled("Event Details"));

    // ── centre layout ──
    JPanel topSection = new JPanel(new BorderLayout(4, 4));
    topSection.setOpaque(false);
    topSection.add(statsPanel, BorderLayout.NORTH);
    topSection.add(listScroll, BorderLayout.CENTER);

    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSection, detailScroll);
    split.setResizeWeight(0.58);
    split.setBorder(null);

    add(split, BorderLayout.CENTER);
  }

  /**
   * Populates the panel with data from the given AnalysisResult.
   * Clears any previous selection.
   *
   * @param result the result to display, must not be null
   */
  public void update(AnalysisResult result) {
    this.anomalies = result.getAnomalies();

    double ret = result.getTotalReturn();
    returnLabel.setText(String.format("Total Return: %+.2f%%", ret));
    returnLabel.setForeground(ret >= 0 ? new Color(40, 167, 69) : new Color(220, 53, 69));

    countLabel.setText(String.format("Anomalies Found: %d", result.getAnomalyCount()));
    summaryArea.setText(result.getSummary());

    listModel.clear();
    for (AnomalyPoint a : anomalies) {
      String arrow = a.isGain() ? "\u25b2" : "\u25bc"; // ▲ / ▼
      listModel.addElement(String.format("%s %s  %+.1f%%  [%d evt]",
          arrow, a.getDate(), a.getPercentChange(), a.getRelatedEvents().size()));
    }
    detailArea.setText("");
  }

  // ── private helpers ───────────────────────────────────────────────────────

  private void onAnomalySelected(int idx) {
    if (anomalies == null || idx < 0 || idx >= anomalies.size()) return;
    AnomalyPoint a = anomalies.get(idx);
    mainFrame.onAnomalySelected(a);

    StringBuilder sb = new StringBuilder();
    sb.append(a.getComment());
    sb.append("\n\nRelated Events:");
    List<MarketEvent> events = a.getRelatedEvents();
    if (events.isEmpty()) {
      sb.append("\n  None found within the event window.");
    } else {
      for (MarketEvent e : events) {
        sb.append("\n\n  [").append(e.getType()).append("]  ").append(e.getDate());
        sb.append("\n  ").append(e.getTitle());
        sb.append("\n  Source: ").append(e.getSource());
      }
    }
    detailArea.setText(sb.toString());
    detailArea.setCaretPosition(0);
  }

  private static TitledBorder titled(String title) {
    return BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(new Color(200, 200, 200)), title);
  }

  // ── inner renderer ────────────────────────────────────────────────────────

  /**
   * Colours each anomaly list entry: green background for gains, red for losses.
   */
  private static class AnomalyListRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {
      Component c = super.getListCellRendererComponent(
          list, value, index, isSelected, cellHasFocus);
      if (!isSelected) {
        String text = value.toString();
        if (text.startsWith("\u25b2")) {        // gain ▲
          c.setBackground(new Color(212, 237, 218));
        } else {                                // loss ▼
          c.setBackground(new Color(248, 215, 218));
        }
      }
      return c;
    }
  }
}
