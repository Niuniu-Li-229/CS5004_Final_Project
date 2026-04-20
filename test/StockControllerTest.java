import controller.StockController;
import model.*;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockControllerTest {

  private StockController controller;

  private static final LocalDate START = LocalDate.of(2025, 9, 2);
  private static final LocalDate END   = LocalDate.of(2025, 9, 5);

  @BeforeEach
  void setUp() {
    controller = new StockController();
  }

  // ── State before data is loaded ───────────────────────────────────────────

  @Test
  void isDataLoaded_beforeLoadData_returnsFalse() {
    assertFalse(controller.isDataLoaded());
  }

  @Test
  void getModel_beforeLoadData_returnsNull() {
    assertNull(controller.getModel());
  }

  @Test
  void getLastResult_beforeLoadData_returnsNull() {
    assertNull(controller.getLastResult());
  }

  // ── analyze: called before data is loaded ────────────────────────────────

  @Test
  void analyze_dataNotLoaded_throwsIllegalState() {
    assertThrows(IllegalStateException.class, () ->
        controller.analyze(START, END, StockController.DEFAULT_THRESHOLD));
  }

  // ── loadData: invalid paths (no file I/O required) ───────────────────────

  @Test
  void loadData_nullPricePath_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        controller.loadData(null, "news.csv"));
  }

  @Test
  void loadData_blankPricePath_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        controller.loadData("  ", "news.csv"));
  }

  // ── analyze: input validation with model injected ────────────────────────

  @Test
  void analyze_nullStart_throwsIllegalArgument() throws Exception {
    injectModel(controller, emptyModel());
    assertThrows(IllegalArgumentException.class, () ->
        controller.analyze(null, END, StockController.DEFAULT_THRESHOLD));
  }

  @Test
  void analyze_nullEnd_throwsIllegalArgument() throws Exception {
    injectModel(controller, emptyModel());
    assertThrows(IllegalArgumentException.class, () ->
        controller.analyze(START, null, StockController.DEFAULT_THRESHOLD));
  }

  @Test
  void analyze_endBeforeStart_throwsIllegalArgument() throws Exception {
    injectModel(controller, emptyModel());
    assertThrows(IllegalArgumentException.class, () ->
        controller.analyze(END, START, StockController.DEFAULT_THRESHOLD)); // reversed
  }

  // ── analyze: empty price range ────────────────────────────────────────────

  @Test
  void analyze_noPricesInRange_returnsResultWithNoAnomalies() throws Exception {
    injectModel(controller, emptyModel());
    AnalysisResult result = controller.analyze(START, END, StockController.DEFAULT_THRESHOLD);
    assertEquals("NVDA",  result.getTicker());
    assertEquals(0,       result.getAnomalyCount());
    assertSame(result,    controller.getLastResult()); // lastResult is updated
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /** Builds an empty model so analyze() guard clauses can be reached without CSV files. */
  private static StockDataModel emptyModel() {
    return new StockDataModel("NVDA", new ArrayList<>(), new ArrayList<>());
  }

  /**
   * Injects a StockDataModel directly into the controller's private field.
   * Avoids requiring real CSV files on disk while respecting the principle
   * of not adding public methods to production code just for testing.
   */
  private static void injectModel(StockController c, StockDataModel m) throws Exception {
    Field f = StockController.class.getDeclaredField("model");
    f.setAccessible(true);
    f.set(c, m);
  }
}
