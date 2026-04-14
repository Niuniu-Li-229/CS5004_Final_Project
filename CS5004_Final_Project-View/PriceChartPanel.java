package stockreview.view;

import stockreview.model.AnalysisResult;
import stockreview.model.AnalysisResult.AnomalyPoint;
import stockreview.model.PricePoint;
import stockreview.model.StockDataModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Custom Swing panel that renders a stock price line chart with volume bars,
 * anomaly markers, hover tooltips, and an optional selection highlight.
 *
 * <p>The chart area is divided vertically: the upper section shows the close
 * price as a line, and the lower VOLUME_HEIGHT pixels show grey volume bars.
 * Anomalous days are marked with coloured circles (green = gain, red = loss).
 * Moving the mouse over the chart shows a tooltip with OHLCV data for that day.
 */
public class PriceChartPanel extends JPanel {

  // ── layout constants ──────────────────────────────────────────────────────
  private static final int PAD_LEFT   = 62;
  private static final int PAD_RIGHT  = 18;
  private static final int PAD_TOP    = 34;
  private static final int PAD_BOTTOM = 46;
  private static final int VOL_HEIGHT = 55;   // pixels for the volume sub-chart
  private static final int X_LABEL_STEP = 18; // draw an x-axis label every N points

  // ── colours ───────────────────────────────────────────────────────────────
  private static final Color C_BG            = Color.WHITE;
  private static final Color C_GRID          = new Color(225, 225, 225);
  private static final Color C_AXIS          = new Color(80, 80, 80);
  private static final Color C_LINE          = new Color(30, 100, 200);
  private static final Color C_VOLUME        = new Color(180, 205, 235, 140);
  private static final Color C_ANOMALY_UP    = new Color(40, 167, 69);
  private static final Color C_ANOMALY_DOWN  = new Color(220, 53, 69);
  private static final Color C_HIGHLIGHT     = new Color(255, 193, 7);
  private static final Color C_TOOLTIP_BG    = new Color(30, 30, 30, 215);

  // ── state ─────────────────────────────────────────────────────────────────
  private List<PricePoint>  prices;
  private List<AnomalyPoint> anomalies;
  private AnomalyPoint      highlighted;
  private String            ticker = "";
  private int               hoverIdx = -1;

  /**
   * Constructs an empty chart panel showing a placeholder message.
   */
  public PriceChartPanel() {
    setBackground(C_BG);
    setPreferredSize(new Dimension(860, 500));
    setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

    MouseAdapter ma = new MouseAdapter() {
      @Override public void mouseMoved(MouseEvent e) { updateHover(e.getX()); }
      @Override public void mouseExited(MouseEvent e) { hoverIdx = -1; repaint(); }
    };
    addMouseMotionListener(ma);
    addMouseListener(ma);
  }

  /**
   * Replaces the displayed data and repaints the chart.
   *
   * @param model  the stock data model (used for the ticker label)
   * @param result the analysis result containing detected anomalies
   * @param start  the display start date (used for range filtering)
   * @param end    the display end date
   */
  public void update(StockDataModel model, AnalysisResult result, LocalDate start, LocalDate end) {
    this.ticker    = model.getTicker();
    this.prices    = model.getPricesInRange(start, end);
    this.anomalies = result.getAnomalies();
    this.highlighted = null;
    this.hoverIdx    = -1;
    repaint();
  }

  /**
   * Highlights one anomaly day with a gold ring.  Pass null to clear.
   *
   * @param anomaly the anomaly to highlight, or null
   */
  public void highlightAnomaly(AnomalyPoint anomaly) {
    this.highlighted = anomaly;
    repaint();
  }

  // ── painting ──────────────────────────────────────────────────────────────

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    g2.setColor(C_BG);
    g2.fillRect(0, 0, getWidth(), getHeight());

    if (prices == null || prices.isEmpty()) {
      drawPlaceholder(g2);
      return;
    }
    drawChart(g2);
  }

  private void drawChart(Graphics2D g2) {
    final int W = getWidth();
    final int H = getHeight();

    final int left        = PAD_LEFT;
    final int right       = W - PAD_RIGHT;
    final int top         = PAD_TOP;
    final int bottom      = H - PAD_BOTTOM;
    final int chartW      = right - left;
    final int priceBottom = bottom - VOL_HEIGHT; // bottom of price area

    final int n = prices.size();

    // ── price bounds ──
    double minP = prices.stream().mapToDouble(PricePoint::getLow).min().orElse(0);
    double maxP = prices.stream().mapToDouble(PricePoint::getHigh).max().orElse(1);
    double rangeP = Math.max(1, maxP - minP);

    // ── volume bounds ──
    long maxVol = prices.stream().mapToLong(PricePoint::getVolume).max().orElse(1);

    // ── horizontal grid + y-axis labels ──
    g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
    int gridLines = 6;
    for (int i = 0; i <= gridLines; i++) {
      int y = priceBottom - (int) ((double) i / gridLines * (priceBottom - top));
      g2.setColor(C_GRID);
      g2.setStroke(dashed());
      g2.drawLine(left, y, right, y);
      g2.setStroke(solid(1f));
      double price = minP + (double) i / gridLines * rangeP;
      g2.setColor(C_AXIS);
      g2.drawString(String.format("%7.1f", price), 2, y + 4);
    }

    // ── volume bars ──
    int barW = Math.max(1, chartW / n - 1);
    for (int i = 0; i < n; i++) {
      int x      = xFor(i, n, left, chartW);
      int barH   = (int) ((double) prices.get(i).getVolume() / maxVol * VOL_HEIGHT);
      g2.setColor(C_VOLUME);
      g2.fillRect(x - barW / 2, bottom - barH, barW, barH);
    }

    // ── price line ──
    g2.setColor(C_LINE);
    g2.setStroke(solid(1.8f));
    for (int i = 1; i < n; i++) {
      g2.drawLine(
          xFor(i - 1, n, left, chartW), yFor(prices.get(i - 1).getClose(), minP, rangeP, top, priceBottom),
          xFor(i,     n, left, chartW), yFor(prices.get(i).getClose(),     minP, rangeP, top, priceBottom));
    }
    g2.setStroke(solid(1f));

    // ── anomaly markers ──
    if (anomalies != null) {
      for (AnomalyPoint a : anomalies) {
        int idx = indexOfDate(a.getDate());
        if (idx < 0) continue;
        int x = xFor(idx, n, left, chartW);
        int y = yFor(a.getPricePoint().getClose(), minP, rangeP, top, priceBottom);
        drawAnomalyMarker(g2, x, y, a);
      }
    }

    // ── x-axis date labels ──
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yy");
    g2.setColor(C_AXIS);
    g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
    for (int i = 0; i < n; i += X_LABEL_STEP) {
      int x = xFor(i, n, left, chartW);
      g2.drawString(prices.get(i).getDate().format(fmt), x - 13, bottom + 13);
    }

    // ── hover tooltip ──
    if (hoverIdx >= 0 && hoverIdx < n)
      drawTooltip(g2, hoverIdx, left, chartW, top, priceBottom, minP, rangeP, W);

    // ── title ──
    g2.setColor(Color.BLACK);
    g2.setFont(new Font("SansSerif", Font.BOLD, 14));
    g2.drawString(ticker + "  Close Price", left, top - 10);

    // ── legend ──
    drawLegend(g2, W, top);
  }

  private void drawAnomalyMarker(Graphics2D g2, int x, int y, AnomalyPoint a) {
    boolean sel = highlighted != null && highlighted.getDate().equals(a.getDate());
    if (sel) {
      g2.setColor(C_HIGHLIGHT);
      g2.fillOval(x - 9, y - 9, 18, 18);
      g2.setColor(Color.BLACK);
      g2.setStroke(solid(1.5f));
      g2.drawOval(x - 9, y - 9, 18, 18);
      g2.setStroke(solid(1f));
    } else {
      Color col = a.isGain() ? C_ANOMALY_UP : C_ANOMALY_DOWN;
      g2.setColor(col);
      g2.fillOval(x - 5, y - 5, 10, 10);
      g2.setColor(col.darker());
      g2.drawOval(x - 5, y - 5, 10, 10);
    }
  }

  private void drawTooltip(Graphics2D g2, int idx, int left, int chartW,
      int top, int priceBottom, double minP, double rangeP, int W) {
    PricePoint p = prices.get(idx);
    int x = xFor(idx, prices.size(), left, chartW);
    int y = yFor(p.getClose(), minP, rangeP, top, priceBottom);

    String[] lines = {
        p.getDate().toString(),
        String.format("O: %,.2f   H: %,.2f", p.getOpen(), p.getHigh()),
        String.format("L: %,.2f   C: %,.2f", p.getLow(),  p.getClose()),
        String.format("Chg: %+.2f%%", p.getOpenToCloseChange()),
        String.format("Vol: %,d", p.getVolume())
    };
    int tw = 178, th = lines.length * 15 + 10;
    int tx = Math.min(x + 10, W - tw - 6);
    int ty = Math.max(y - th / 2, 4);

    g2.setColor(C_TOOLTIP_BG);
    g2.fillRoundRect(tx, ty, tw, th, 7, 7);
    g2.setColor(Color.WHITE);
    g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
    for (int i = 0; i < lines.length; i++)
      g2.drawString(lines[i], tx + 6, ty + 14 + i * 15);

    // vertical crosshair
    g2.setColor(new Color(100, 100, 100, 120));
    g2.setStroke(dashed());
    g2.drawLine(x, top, x, priceBottom);
    g2.setStroke(solid(1f));

    g2.setColor(C_LINE);
    g2.fillOval(x - 3, y - 3, 6, 6);
  }

  private void drawLegend(Graphics2D g2, int W, int top) {
    int x = W - 155, y = top + 4;
    g2.setFont(new Font("SansSerif", Font.PLAIN, 11));

    g2.setColor(C_ANOMALY_UP);
    g2.fillOval(x, y + 1, 10, 10);
    g2.setColor(Color.DARK_GRAY);
    g2.drawString("Gain anomaly", x + 14, y + 11);

    y += 18;
    g2.setColor(C_ANOMALY_DOWN);
    g2.fillOval(x, y + 1, 10, 10);
    g2.setColor(Color.DARK_GRAY);
    g2.drawString("Loss anomaly", x + 14, y + 11);

    y += 18;
    g2.setColor(C_HIGHLIGHT);
    g2.fillOval(x, y + 1, 10, 10);
    g2.setColor(Color.DARK_GRAY);
    g2.drawString("Selected",     x + 14, y + 11);
  }

  private void drawPlaceholder(Graphics2D g2) {
    g2.setColor(new Color(160, 160, 160));
    g2.setFont(new Font("SansSerif", Font.ITALIC, 15));
    String msg = "Load data and click Analyze to view the chart";
    int sw = g2.getFontMetrics().stringWidth(msg);
    g2.drawString(msg, (getWidth() - sw) / 2, getHeight() / 2);
  }

  // ── coordinate helpers ────────────────────────────────────────────────────

  private int xFor(int i, int n, int left, int chartW) {
    if (n <= 1) return left + chartW / 2;
    return left + (int) ((double) i / (n - 1) * chartW);
  }

  private int yFor(double price, double minP, double rangeP, int top, int priceBottom) {
    return priceBottom - (int) ((price - minP) / rangeP * (priceBottom - top));
  }

  private int indexOfDate(LocalDate date) {
    if (prices == null) return -1;
    for (int i = 0; i < prices.size(); i++)
      if (prices.get(i).getDate().equals(date)) return i;
    return -1;
  }

  private void updateHover(int mouseX) {
    if (prices == null || prices.isEmpty()) return;
    int n      = prices.size();
    int left   = PAD_LEFT;
    int chartW = getWidth() - PAD_LEFT - PAD_RIGHT;
    if (n <= 1) { hoverIdx = 0; repaint(); return; }
    double step = (double) chartW / (n - 1);
    int    idx  = (int) Math.round((mouseX - left) / step);
    hoverIdx = Math.max(0, Math.min(n - 1, idx));
    repaint();
  }

  // ── stroke factories ──────────────────────────────────────────────────────

  private static BasicStroke solid(float w) {
    return new BasicStroke(w);
  }

  private static BasicStroke dashed() {
    return new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
        0, new float[]{3f, 3f}, 0);
  }
}
