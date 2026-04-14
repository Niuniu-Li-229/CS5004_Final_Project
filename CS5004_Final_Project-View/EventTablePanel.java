package stockreview.view;

import stockreview.model.EventType;
import stockreview.model.MarketEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Bottom panel that shows market events in a filterable, colour-coded table.
 * Each row represents one MarketEvent; row colours reflect the EventType.
 * A drop-down selector above the table lets the user filter by event type,
 * and the table can be scrolled programmatically to a specific date when the
 * user selects an anomaly in the AnalysisPanel.
 */
public class EventTablePanel extends JPanel {

  private static final String[] COLUMNS = {"Date", "Type", "Headline", "Source"};

  /** Background colours per EventType for table rows. */
  private static final Map<EventType, Color> TYPE_COLORS;
  static {
    Map<EventType, Color> m = new EnumMap<>(EventType.class);
    m.put(EventType.EARNINGS,   new Color(255, 243, 205));
    m.put(EventType.ANALYST,    new Color(207, 226, 255));
    m.put(EventType.REGULATORY, new Color(255, 220, 220));
    m.put(EventType.MACRO,      new Color(255, 235, 205));
    m.put(EventType.PRODUCT,    new Color(212, 237, 218));
    m.put(EventType.OTHER,      new Color(240, 240, 240));
    TYPE_COLORS = Collections.unmodifiableMap(m);
  }

  private final DefaultTableModel  tableModel;
  private final JTable             table;
  private final JComboBox<String>  filterBox;
  private final JLabel             countLabel;

  private List<MarketEvent> allEvents = new ArrayList<>();
  private LocalDate         rangeStart;
  private LocalDate         rangeEnd;

  /**
   * Constructs the EventTablePanel with an empty table and filter toolbar.
   */
  public EventTablePanel() {
    setLayout(new BorderLayout(4, 4));
    setBorder(new EmptyBorder(2, 4, 2, 4));
    setPreferredSize(new Dimension(1280, 200));

    // ── table model (read-only) ──
    tableModel = new DefaultTableModel(COLUMNS, 0) {
      @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    table = new JTable(tableModel);
    table.setFont(new Font("SansSerif", Font.PLAIN, 12));
    table.setRowHeight(22);
    table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
    table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getColumnModel().getColumn(0).setPreferredWidth(95);
    table.getColumnModel().getColumn(1).setPreferredWidth(95);
    table.getColumnModel().getColumn(2).setPreferredWidth(700);
    table.getColumnModel().getColumn(3).setPreferredWidth(95);
    table.setDefaultRenderer(Object.class, new EventRowRenderer());

    JScrollPane scroll = new JScrollPane(table);
    scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

    // ── filter toolbar ──
    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
    toolbar.setBackground(new Color(245, 245, 245));

    toolbar.add(new JLabel("Filter by Type:"));
    String[] options = {"All", "EARNINGS", "ANALYST", "REGULATORY", "MACRO", "PRODUCT", "OTHER"};
    filterBox = new JComboBox<>(options);
    filterBox.addActionListener(e -> applyFilter());
    toolbar.add(filterBox);

    countLabel = new JLabel("0 events");
    countLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
    toolbar.add(countLabel);

    add(toolbar, BorderLayout.NORTH);
    add(scroll,  BorderLayout.CENTER);
  }

  /**
   * Replaces the displayed events with those in the given list,
   * filtered to the provided date range.
   *
   * @param events all market events to display (may exceed the date range)
   * @param start  the display start date (inclusive); pass null to show all
   * @param end    the display end date (inclusive); pass null to show all
   */
  public void update(List<MarketEvent> events, LocalDate start, LocalDate end) {
    this.allEvents  = new ArrayList<>(events);
    this.rangeStart = start;
    this.rangeEnd   = end;
    filterBox.setSelectedIndex(0); // reset to "All" and repopulate
    applyFilter();
  }

  /**
   * Selects and scrolls to the first row whose date matches the given date.
   * Has no effect if no matching row exists.
   *
   * @param date the date to scroll to
   */
  public void highlightDate(LocalDate date) {
    for (int r = 0; r < tableModel.getRowCount(); r++) {
      Object val = tableModel.getValueAt(r, 0);
      if (val != null && val.toString().equals(date.toString())) {
        table.setRowSelectionInterval(r, r);
        table.scrollRectToVisible(table.getCellRect(r, 0, true));
        return;
      }
    }
  }

  // ── private helpers ───────────────────────────────────────────────────────

  /** Repopulates the table applying both the date range and type filter. */
  private void applyFilter() {
    String selected = (String) filterBox.getSelectedItem();
    tableModel.setRowCount(0);

    List<MarketEvent> filtered = new ArrayList<>();
    for (MarketEvent e : allEvents) {
      // date range filter
      if (rangeStart != null && e.getDate().isBefore(rangeStart)) continue;
      if (rangeEnd   != null && e.getDate().isAfter(rangeEnd))    continue;
      // type filter
      if (!"All".equals(selected) && !e.getType().name().equals(selected)) continue;
      filtered.add(e);
    }

    // sort descending by date (most recent first)
    filtered.sort((a, b) -> b.getDate().compareTo(a.getDate()));

    for (MarketEvent e : filtered) {
      tableModel.addRow(new Object[]{
          e.getDate().toString(),
          e.getType().name(),
          e.getTitle(),
          e.getSource()
      });
    }
    countLabel.setText(filtered.size() + " event" + (filtered.size() == 1 ? "" : "s"));
  }

  // ── inner renderer ────────────────────────────────────────────────────────

  /** Colours table rows by EventType; selected rows keep the default highlight. */
  private static class EventRowRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable t, Object value,
        boolean selected, boolean focused, int row, int col) {
      Component c = super.getTableCellRendererComponent(t, value, selected, focused, row, col);
      if (!selected) {
        String typeName = Objects.toString(t.getModel().getValueAt(row, 1), "");
        try {
          EventType type = EventType.valueOf(typeName);
          c.setBackground(TYPE_COLORS.getOrDefault(type, Color.WHITE));
        } catch (IllegalArgumentException ignored) {
          c.setBackground(Color.WHITE);
        }
      }
      return c;
    }
  }
}
