package view;

import model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom Swing panel that renders a line chart of TSLA closing prices
 * over a date range. Anomaly days are overlaid as coloured circles:
 * green for gains and red for losses. Mouse hover shows a tooltip with
 * the exact date, closing price, and anomaly details when applicable.
 */
public class PriceChartPanel extends JPanel {

  // Layout margins in pixels
  private static final int MARGIN_TOP    = 30;
  private static final int MARGIN_BOTTOM = 50;
  private static final int MARGIN_LEFT   = 70;
  private static final int MARGIN_RIGHT  = 20;

  // Visual constants
  private static final int    ANOMALY_DOT_RADIUS = 5;
  private static final Color  COLOR_GAIN         = new Color(46, 160, 87);
  private static final Color  COLOR_LOSS         = new Color(218, 54, 51);
  private static final Color  COLOR_LINE         = new Color(31, 111, 235);
  private static final Color  COLOR_GRID         = new Color(220, 220, 220);
  private static final Color  COLOR_TOOLTIP_BG   = new Color(30, 30, 30, 210);
  private static final Color  COLOR_BG           = Color.WHITE;
  private static final Font   FONT_AXIS          = new Font("SansSerif", Font.PLAIN, 11);
  private static final Font   FONT_TITLE         = new Font("SansSerif", Font.BOLD, 13);

  private List<PricePoint>              prices    = new ArrayList<>();
  private List<AnalysisResult.AnomalyPoint> anomalies = new ArrayList<>();

  // Mouse-hover state
  private int    hoverIndex = -1;
  private String hoverText  = "";

  /**
   * Constructs an empty PriceChartPanel. Call {@link #setData} to populate it.
   * Sets up mouse motion listener for hover tooltips.
   */
  public PriceChartPanel() {
    setBackground(COLOR_BG);
    setPreferredSize(new Dimension(800, 400));
    addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) { updateHover(e.getX()); }
    });
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        hoverIndex = -1;
        hoverText  = "";
        repaint();
      }
    });
  }

  /**
   * Replaces the displayed data and triggers a repaint.
   * Passing empty lists clears the chart.
   *
   * @param prices    ordered list of price points to plot, must not be null
   * @param anomalies list of anomaly points to overlay, must not be null
   */
  public void setData(List<PricePoint> prices,
                      List<AnalysisResult.AnomalyPoint> anomalies) {
    this.prices    = new ArrayList<>(prices);
    this.anomalies = new ArrayList<>(anomalies);
    this.hoverIndex = -1;
    this.hoverText  = "";
    repaint();
  }

  // -------------------------------------------------------------------------
  // Painting
  // -------------------------------------------------------------------------

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int w = getWidth();
    int h = getHeight();

    if (prices.isEmpty()) {
      g2.setColor(Color.GRAY);
      g2.setFont(FONT_TITLE);
      String msg = "No data to display. Run an analysis to see the price chart.";
      FontMetrics fm = g2.getFontMetrics();
      g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
      return;
    }

    int chartX = MARGIN_LEFT;
    int chartY = MARGIN_TOP;
    int chartW = w - MARGIN_LEFT - MARGIN_RIGHT;
    int chartH = h - MARGIN_TOP - MARGIN_BOTTOM;

    double minPrice = prices.stream().mapToDouble(PricePoint::getLow).min().orElse(0);
    double maxPrice = prices.stream().mapToDouble(PricePoint::getHigh).max().orElse(1);
    double priceRange = maxPrice - minPrice;
    if (priceRange == 0) priceRange = 1;

    LocalDate firstDate = prices.get(0).getDate();
    LocalDate lastDate  = prices.get(prices.size() - 1).getDate();
    long totalDays = lastDate.toEpochDay() - firstDate.toEpochDay();
    if (totalDays == 0) totalDays = 1;

    drawGrid(g2, chartX, chartY, chartW, chartH, minPrice, maxPrice,
             priceRange, firstDate, totalDays);
    drawTitle(g2, w, firstDate, lastDate);
    drawAxes(g2, chartX, chartY, chartW, chartH, minPrice, maxPrice,
             priceRange, firstDate, lastDate, totalDays);
    drawPriceLine(g2, chartX, chartY, chartW, chartH,
                  minPrice, priceRange, firstDate, totalDays);
    drawAnomalyDots(g2, chartX, chartY, chartW, chartH,
                    minPrice, priceRange, firstDate, totalDays);

    if (hoverIndex >= 0) {
      drawHoverLine(g2, chartX, chartY, chartW, chartH,
                    minPrice, priceRange, firstDate, totalDays);
    }
  }

  /** Draws horizontal grid lines with price labels on the Y axis. */
  private void drawGrid(Graphics2D g2, int cx, int cy, int cw, int ch,
                        double minPrice, double maxPrice, double priceRange,
                        LocalDate firstDate, long totalDays) {
    g2.setFont(FONT_AXIS);
    g2.setColor(COLOR_GRID);
    int gridLines = 6;
    for (int i = 0; i <= gridLines; i++) {
      double price = minPrice + priceRange * i / gridLines;
      int y = cy + ch - (int) ((price - minPrice) / priceRange * ch);
      g2.setColor(COLOR_GRID);
      g2.drawLine(cx, y, cx + cw, y);
      g2.setColor(Color.DARK_GRAY);
      g2.drawString(String.format("$%.0f", price), cx - 55, y + 4);
    }
  }

  /** Draws chart title. */
  private void drawTitle(Graphics2D g2, int w, LocalDate start, LocalDate end) {
    g2.setFont(FONT_TITLE);
    g2.setColor(Color.DARK_GRAY);
    String title = String.format("Closing Price  (%s → %s)", start, end);
    FontMetrics fm = g2.getFontMetrics();
    g2.drawString(title, (w - fm.stringWidth(title)) / 2, MARGIN_TOP - 8);
  }

  /** Draws X-axis date labels at roughly yearly intervals. */
  private void drawAxes(Graphics2D g2, int cx, int cy, int cw, int ch,
                        double minPrice, double maxPrice, double priceRange,
                        LocalDate firstDate, LocalDate lastDate, long totalDays) {
    g2.setColor(Color.DARK_GRAY);
    g2.drawLine(cx, cy + ch, cx + cw, cy + ch); // X axis
    g2.drawLine(cx, cy,       cx,      cy + ch); // Y axis

    int labelCount = Math.min(8, (int) (totalDays / 60) + 1);
    if (labelCount < 1) labelCount = 1;
    g2.setFont(FONT_AXIS);
    FontMetrics fm = g2.getFontMetrics();

    for (int i = 0; i <= labelCount; i++) {
      long   dayOffset = totalDays * i / labelCount;
      LocalDate lbl    = firstDate.plusDays(dayOffset);
      int    x         = cx + (int) (dayOffset * cw / totalDays);
      g2.setColor(COLOR_GRID);
      g2.drawLine(x, cy, x, cy + ch);
      g2.setColor(Color.DARK_GRAY);
      String[] parts = lbl.format(DateTimeFormatter.ofPattern("MMM yyyy")).split(" ");
      g2.drawString(parts[0], x - fm.stringWidth(parts[0]) / 2, cy + ch + 15);
      if (parts.length > 1)
        g2.drawString(parts[1], x - fm.stringWidth(parts[1]) / 2, cy + ch + 28);
    }
  }

  /** Draws the closing price as a blue line. */
  private void drawPriceLine(Graphics2D g2, int cx, int cy, int cw, int ch,
                             double minPrice, double priceRange,
                             LocalDate firstDate, long totalDays) {
    g2.setColor(COLOR_LINE);
    g2.setStroke(new BasicStroke(1.5f));

    int prevX = -1, prevY = -1;
    for (PricePoint p : prices) {
      int x = toX(cx, cw, firstDate, totalDays, p.getDate());
      int y = toY(cy, ch, minPrice, priceRange, p.getClose());
      if (prevX >= 0) g2.drawLine(prevX, prevY, x, y);
      prevX = x; prevY = y;
    }
    g2.setStroke(new BasicStroke(1f));
  }

  /** Draws coloured circles on anomaly days. */
  private void drawAnomalyDots(Graphics2D g2, int cx, int cy, int cw, int ch,
                               double minPrice, double priceRange,
                               LocalDate firstDate, long totalDays) {
    for (AnalysisResult.AnomalyPoint ap : anomalies) {
      int x = toX(cx, cw, firstDate, totalDays, ap.getDate());
      int y = toY(cy, ch, minPrice, priceRange, ap.getPricePoint().getClose());
      g2.setColor(ap.isGain() ? COLOR_GAIN : COLOR_LOSS);
      g2.fillOval(x - ANOMALY_DOT_RADIUS, y - ANOMALY_DOT_RADIUS,
                  ANOMALY_DOT_RADIUS * 2, ANOMALY_DOT_RADIUS * 2);
      g2.setColor(Color.WHITE);
      g2.setStroke(new BasicStroke(1f));
      g2.drawOval(x - ANOMALY_DOT_RADIUS, y - ANOMALY_DOT_RADIUS,
                  ANOMALY_DOT_RADIUS * 2, ANOMALY_DOT_RADIUS * 2);
    }
  }

  /** Draws the hover vertical line and tooltip box. */
  private void drawHoverLine(Graphics2D g2, int cx, int cy, int cw, int ch,
                             double minPrice, double priceRange,
                             LocalDate firstDate, long totalDays) {
    PricePoint p = prices.get(hoverIndex);
    int x = toX(cx, cw, firstDate, totalDays, p.getDate());
    int y = toY(cy, ch, minPrice, priceRange, p.getClose());

    g2.setColor(new Color(100, 100, 100, 150));
    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
        0, new float[]{4}, 0));
    g2.drawLine(x, cy, x, cy + ch);
    g2.setStroke(new BasicStroke(1f));

    // Tooltip box
    String[] lines = hoverText.split("\n");
    g2.setFont(FONT_AXIS);
    FontMetrics fm = g2.getFontMetrics();
    int pad   = 6;
    int lineH = fm.getHeight();
    int boxW  = 0;
    for (String l : lines) boxW = Math.max(boxW, fm.stringWidth(l));
    boxW += pad * 2;
    int boxH = lines.length * lineH + pad * 2;

    int tx = x + 10;
    int ty = cy + 10;
    if (tx + boxW > cx + cw) tx = x - boxW - 10;
    if (ty + boxH > cy + ch) ty = cy + ch - boxH - 5;

    g2.setColor(COLOR_TOOLTIP_BG);
    g2.fillRoundRect(tx, ty, boxW, boxH, 6, 6);
    g2.setColor(Color.WHITE);
    for (int i = 0; i < lines.length; i++)
      g2.drawString(lines[i], tx + pad, ty + pad + fm.getAscent() + i * lineH);

    // Dot at hover point
    g2.setColor(COLOR_LINE);
    g2.fillOval(x - 4, y - 4, 8, 8);
  }

  // -------------------------------------------------------------------------
  // Mouse hover helpers
  // -------------------------------------------------------------------------

  /** Finds the price point closest to the given pixel X and updates hover state. */
  private void updateHover(int mouseX) {
    if (prices.isEmpty()) return;
    int cw = getWidth() - MARGIN_LEFT - MARGIN_RIGHT;
    if (cw <= 0) return;

    LocalDate firstDate = prices.get(0).getDate();
    LocalDate lastDate  = prices.get(prices.size() - 1).getDate();
    long totalDays = lastDate.toEpochDay() - firstDate.toEpochDay();
    if (totalDays == 0) return;

    int best = 0;
    int bestDist = Integer.MAX_VALUE;
    for (int i = 0; i < prices.size(); i++) {
      int px = toX(MARGIN_LEFT, cw, firstDate, totalDays, prices.get(i).getDate());
      int d  = Math.abs(px - mouseX);
      if (d < bestDist) { bestDist = d; best = i; }
    }

    if (bestDist > 20) {
      hoverIndex = -1;
      hoverText  = "";
    } else {
      hoverIndex = best;
      PricePoint p = prices.get(best);
      StringBuilder sb = new StringBuilder();
      sb.append(p.getDate()).append("\n");
      sb.append(String.format("Close: $%.2f", p.getClose())).append("\n");
      sb.append(String.format("Range: $%.2f – $%.2f", p.getLow(), p.getHigh()));

      for (AnalysisResult.AnomalyPoint ap : anomalies) {
        if (ap.getDate().equals(p.getDate())) {
          sb.append(String.format("\nAnomaly: %+.2f%%", ap.getPercentChange()));
          break;
        }
      }
      hoverText = sb.toString();
    }
    repaint();
  }

  // -------------------------------------------------------------------------
  // Coordinate conversion helpers
  // -------------------------------------------------------------------------

  /** Maps a date to a pixel X position within the chart area. */
  private int toX(int cx, int cw, LocalDate firstDate, long totalDays, LocalDate date) {
    long offset = date.toEpochDay() - firstDate.toEpochDay();
    return cx + (int) (offset * cw / totalDays);
  }

  /** Maps a price value to a pixel Y position within the chart area (inverted). */
  private int toY(int cy, int ch, double minPrice, double priceRange, double price) {
    return cy + ch - (int) ((price - minPrice) / priceRange * ch);
  }
}
