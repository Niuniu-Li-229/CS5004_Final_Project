package view;

import model.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A Swing panel that displays {@link MarketEvent} objects in a filterable
 * {@link JTable}. A {@link JComboBox} at the top lets the user narrow the
 * list to a specific {@link EventType} or view all events at once. Selecting
 * a row shows the full event description in a text area below the table.
 */
public class EventTablePanel extends JPanel {

  private static final String[] COLUMNS = {"Date", "Type", "Headline", "Source"};

  private List<MarketEvent> allEvents     = new ArrayList<>();
  private List<MarketEvent> filteredEvents = new ArrayList<>();

  private final EventTableModel tableModel;
  private final JComboBox<String> filterBox;
  private final JTextArea detailArea;

  /**
   * Constructs an empty EventTablePanel. Call {@link #setEvents} to populate it.
   * Initialises the filter combo box, table, and detail area.
   */
  public EventTablePanel() {
    setLayout(new BorderLayout(0, 4));

    // ---- Filter bar -------------------------------------------------------
    filterBox = new JComboBox<>();
    filterBox.addItem("ALL");
    for (EventType t : EventType.values()) filterBox.addItem(t.name());
    filterBox.addActionListener(e -> applyFilter());

    JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    filterBar.add(new JLabel("Filter by type:"));
    filterBar.add(filterBox);

    JLabel countLabel = new JLabel();
    filterBar.add(countLabel);
    add(filterBar, BorderLayout.NORTH);

    // ---- Table ------------------------------------------------------------
    tableModel = new EventTableModel();
    JTable table = new JTable(tableModel);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setRowHeight(22);
    table.getColumnModel().getColumn(0).setPreferredWidth(90);
    table.getColumnModel().getColumn(1).setPreferredWidth(100);
    table.getColumnModel().getColumn(2).setPreferredWidth(500);
    table.getColumnModel().getColumn(3).setPreferredWidth(110);

    // Colour the Type cell to match event category
    table.getColumnModel().getColumn(1).setCellRenderer(new TypeCellRenderer());

    table.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        int row = table.getSelectedRow();
        if (row >= 0 && row < filteredEvents.size()) {
          showDetail(filteredEvents.get(row));
        }
      }
    });

    JScrollPane tableScroll = new JScrollPane(table);
    tableScroll.setPreferredSize(new Dimension(800, 300));

    // ---- Detail area ------------------------------------------------------
    detailArea = new JTextArea(5, 80);
    detailArea.setEditable(false);
    detailArea.setLineWrap(true);
    detailArea.setWrapStyleWord(true);
    detailArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
    detailArea.setBackground(new Color(248, 248, 248));
    detailArea.setText("Select a row to see the full event description.");
    JScrollPane detailScroll = new JScrollPane(detailArea);
    detailScroll.setBorder(BorderFactory.createTitledBorder("Event Detail"));

    // ---- Split pane -------------------------------------------------------
    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, detailScroll);
    split.setResizeWeight(0.75);
    split.setDividerSize(5);
    add(split, BorderLayout.CENTER);
  }

  /**
   * Replaces the full events list and refreshes the table.
   * Resets the filter selection to "ALL".
   *
   * @param events the new list of events to display, must not be null
   */
  public void setEvents(List<MarketEvent> events) {
    this.allEvents = new ArrayList<>(events);
    filterBox.setSelectedIndex(0);
    applyFilter();
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  /** Applies the currently selected EventType filter and refreshes the table. */
  private void applyFilter() {
    String selected = (String) filterBox.getSelectedItem();
    filteredEvents = new ArrayList<>();
    for (MarketEvent e : allEvents) {
      if ("ALL".equals(selected) || e.getType().name().equals(selected))
        filteredEvents.add(e);
    }
    tableModel.fireTableDataChanged();
    detailArea.setText("Select a row to see the full event description.");
  }

  /** Populates the detail area with full info for the selected event. */
  private void showDetail(MarketEvent e) {
    String text = String.format(
        "Date:    %s%nType:    %s%nSource:  %s%n%nHeadline:%n%s%n%nDescription:%n%s",
        e.getDate(), e.getType(), e.getSource(), e.getTitle(), e.getDescription());
    detailArea.setText(text);
    detailArea.setCaretPosition(0);
  }

  // -------------------------------------------------------------------------
  // Inner classes
  // -------------------------------------------------------------------------

  /**
   * AbstractTableModel backed by the filteredEvents list.
   * Returns read-only string data for each cell.
   */
  private class EventTableModel extends AbstractTableModel {

    /**
     * Returns the number of rows currently displayed after filtering.
     * @return filtered row count
     */
    @Override public int getRowCount()    { return filteredEvents.size(); }

    /**
     * Returns the fixed number of columns in this table.
     * @return always 4 (Date, Type, Headline, Source)
     */
    @Override public int getColumnCount() { return COLUMNS.length; }

    /**
     * Returns the column header name for the given index.
     * @param col column index (0–3)
     * @return column name string
     */
    @Override public String getColumnName(int col) { return COLUMNS[col]; }

    /**
     * Returns the display value for a given cell.
     *
     * @param row the row index within the filtered list
     * @param col the column index (0=Date, 1=Type, 2=Headline, 3=Source)
     * @return a String representation of the cell value
     */
    @Override
    public Object getValueAt(int row, int col) {
      MarketEvent e = filteredEvents.get(row);
      switch (col) {
        case 0: return e.getDate().toString();
        case 1: return e.getType().name();
        case 2: return e.getTitle();
        case 3: return e.getSource();
        default: return "";
      }
    }
  }

  /**
   * Cell renderer that colours the Type column text to visually distinguish
   * event categories from one another.
   */
  private static class TypeCellRenderer extends DefaultTableCellRenderer {

    /**
     * Returns a label whose foreground colour reflects the event type.
     *
     * @param table      the owning JTable
     * @param value      the cell value (EventType name string)
     * @param isSelected whether this row is selected
     * @param hasFocus   whether this cell has keyboard focus
     * @param row        the row index
     * @param column     the column index
     * @return the configured renderer component
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (!isSelected) {
        String type = value == null ? "" : value.toString();
        switch (type) {
          case "EARNINGS":   setForeground(new Color(130, 50,  180)); break;
          case "ANALYST":    setForeground(new Color(30,  100, 200)); break;
          case "REGULATORY": setForeground(new Color(200, 80,  40));  break;
          case "MACRO":      setForeground(new Color(180, 130, 0));   break;
          case "PRODUCT":    setForeground(new Color(40,  140, 60));  break;
          default:           setForeground(Color.DARK_GRAY);          break;
        }
      } else {
        setForeground(Color.WHITE);
      }
      return this;
    }
  }
}
