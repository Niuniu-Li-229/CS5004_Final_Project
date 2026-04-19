package view;

import model.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A Swing panel that presents the result of a stock analysis run.
 * A summary banner at the top shows key statistics (total return,
 * anomaly count, date range). The left side lists every anomaly day;
 * selecting one populates the right side with the price details and
 * any related news events found within the search window.
 */
public class AnalysisPanel extends JPanel {

  private static final Color COLOR_GAIN    = new Color(46,  160, 87);
  private static final Color COLOR_LOSS    = new Color(218, 54,  51);
  private static final Color COLOR_NEUTRAL = new Color(100, 100, 100);

  private List<AnalysisResult.AnomalyPoint> anomalies = new ArrayList<>();

  private final JLabel summaryLabel;
  private final DefaultListModel<String>  listModel;
  private final JList<String>             anomalyList;
  private final JTextArea                 detailArea;

  /**
   * Constructs an empty AnalysisPanel. Call {@link #setResult} to populate it.
   * Lays out the summary banner, anomaly list, and detail pane.
   */
  public AnalysisPanel() {
    setLayout(new BorderLayout(0, 6));

    // ---- Summary banner ---------------------------------------------------
    summaryLabel = new JLabel("Run an analysis to see results.");
    summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
    summaryLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
    summaryLabel.setOpaque(true);
    summaryLabel.setBackground(new Color(240, 244, 255));
    add(summaryLabel, BorderLayout.NORTH);

    // ---- Anomaly list (left) ----------------------------------------------
    listModel    = new DefaultListModel<>();
    anomalyList  = new JList<>(listModel);
    anomalyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    anomalyList.setFont(new Font("Monospaced", Font.PLAIN, 12));
    anomalyList.setCellRenderer(new AnomalyCellRenderer());
    anomalyList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) showDetail(anomalyList.getSelectedIndex());
    });

    JScrollPane listScroll = new JScrollPane(anomalyList);
    listScroll.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), "Anomaly Days",
        TitledBorder.LEFT, TitledBorder.TOP,
        new Font("SansSerif", Font.BOLD, 12)));
    listScroll.setPreferredSize(new Dimension(260, 400));

    // ---- Detail area (right) ----------------------------------------------
    detailArea = new JTextArea();
    detailArea.setEditable(false);
    detailArea.setLineWrap(true);
    detailArea.setWrapStyleWord(true);
    detailArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
    detailArea.setBackground(new Color(252, 252, 252));
    detailArea.setText("Select an anomaly day on the left to see details.");

    JScrollPane detailScroll = new JScrollPane(detailArea);
    detailScroll.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), "Anomaly Detail & Related News",
        TitledBorder.LEFT, TitledBorder.TOP,
        new Font("SansSerif", Font.BOLD, 12)));

    // ---- Split pane -------------------------------------------------------
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, detailScroll);
    split.setDividerLocation(270);
    split.setResizeWeight(0.3);
    split.setDividerSize(5);
    add(split, BorderLayout.CENTER);
  }

  /**
   * Populates the panel with the given analysis result and refreshes all
   * sub-components. Passing null clears the panel.
   *
   * @param result the {@link AnalysisResult} to display, or null to clear
   */
  public void setResult(AnalysisResult result) {
    listModel.clear();
    detailArea.setText("Select an anomaly day on the left to see details.");

    if (result == null) {
      summaryLabel.setText("Run an analysis to see results.");
      summaryLabel.setForeground(COLOR_NEUTRAL);
      anomalies = new ArrayList<>();
      return;
    }

    anomalies = new ArrayList<>(result.getAnomalies());

    // Summary banner
    double ret = result.getTotalReturn();
    String returnStr = String.format("%+.2f%%", ret);
    summaryLabel.setText(String.format(
        "  %s  %s → %s  |  Total return: %s  |  Anomaly days: %d",
        result.getTicker(), result.getStartDate(), result.getEndDate(),
        returnStr, result.getAnomalyCount()));
    summaryLabel.setForeground(ret >= 0 ? COLOR_GAIN : COLOR_LOSS);

    // Populate anomaly list
    for (AnalysisResult.AnomalyPoint ap : anomalies) {
      String label = String.format("%s   %+7.2f%%   %s",
          ap.getDate(),
          ap.getPercentChange(),
          ap.isGain() ? "▲" : "▼");
      listModel.addElement(label);
    }
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  /** Builds and displays the detail text for the anomaly at the given index. */
  private void showDetail(int index) {
    if (index < 0 || index >= anomalies.size()) return;

    AnalysisResult.AnomalyPoint ap = anomalies.get(index);
    PricePoint p = ap.getPricePoint();

    StringBuilder sb = new StringBuilder();
    sb.append("═══════════════════════════════════════════════════\n");
    sb.append(String.format("  Date:        %s%n", ap.getDate()));
    sb.append(String.format("  Change:      %+.2f%%%n", ap.getPercentChange()));
    sb.append(String.format("  Close:       $%.2f%n", p.getClose()));
    sb.append(String.format("  Open:        $%.2f%n", p.getOpen()));
    sb.append(String.format("  Day Range:   $%.2f – $%.2f%n", p.getLow(), p.getHigh()));
    sb.append(String.format("  Volume:      %,d%n", p.getVolume()));
    sb.append("═══════════════════════════════════════════════════\n");
    sb.append(String.format("%n  Comment: %s%n", ap.getComment()));

    List<MarketEvent> events = ap.getRelatedEvents();
    if (events.isEmpty()) {
      sb.append("\n  No nearby news events found.\n");
    } else {
      sb.append(String.format("%n  Related News (%d event(s) within ±3 days):%n", events.size()));
      sb.append("  ─────────────────────────────────────────────────\n");
      for (MarketEvent e : events) {
        sb.append(String.format("  [%s] %s%n", e.getType(), e.getDate()));
        sb.append(String.format("  %s%n", e.getTitle()));
        sb.append(String.format("  Source: %s%n", e.getSource()));
        if (!e.getDescription().isBlank())
          sb.append(String.format("  %s%n", e.getDescription()));
        sb.append("\n");
      }
    }

    detailArea.setText(sb.toString());
    detailArea.setCaretPosition(0);
  }

  // -------------------------------------------------------------------------
  // Inner classes
  // -------------------------------------------------------------------------

  /**
   * Custom list cell renderer that colours gain rows green and loss rows red,
   * making it easy to scan the anomaly list at a glance.
   */
  private class AnomalyCellRenderer extends DefaultListCellRenderer {

    /**
     * Returns a label coloured according to whether the anomaly was a gain or loss.
     *
     * @param list         the owning JList
     * @param value        the list cell value (formatted string)
     * @param index        the cell index
     * @param isSelected   whether this cell is selected
     * @param cellHasFocus whether this cell has keyboard focus
     * @return the configured renderer label
     */
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (!isSelected && index < anomalies.size()) {
        boolean gain = anomalies.get(index).isGain();
        setForeground(gain ? COLOR_GAIN : COLOR_LOSS);
        setBackground(gain ? new Color(240, 255, 244) : new Color(255, 242, 242));
      }
      setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
      return this;
    }
  }
}
