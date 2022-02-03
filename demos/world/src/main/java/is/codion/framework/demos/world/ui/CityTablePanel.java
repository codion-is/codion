package is.codion.framework.demos.world.ui;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.PieDataset;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.util.List;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static org.jfree.chart.ChartFactory.createPieChart;

final class CityTablePanel extends EntityTablePanel {

  private final CityTableModel cityTableModel;

  CityTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    this.cityTableModel = (CityTableModel) tableModel;
  }

  @Override
  protected void layoutPanel(JPanel tablePanel, JPanel southPanel) {
    JPanel tableViewPanel = new JPanel(borderLayout());
    tableViewPanel.add(tablePanel, BorderLayout.CENTER);
    tableViewPanel.add(southPanel, BorderLayout.SOUTH);
    ChartPanel cityChartPanel = createChartPanel("Cities", cityTableModel.getChartDataset());
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Table", tableViewPanel);
    tabbedPane.addTab("Chart", cityChartPanel);
    setLayout(borderLayout());
    add(tabbedPane, BorderLayout.CENTER);
  }

  @Override
  protected Controls getPopupControls(List<Controls> additionalPopupControls) {
    return super.getPopupControls(additionalPopupControls)
            .addAt(0, createFetchLocationControl())
            .addSeparatorAt(1);
  }

  private Control createFetchLocationControl() {
    return Control.builder(this::fetchLocation)
            .caption("Fetch location")
            .enabledState(cityTableModel.getCitiesWithoutLocationSelectedObserver())
            .build();
  }

  private void fetchLocation() {
    FetchLocationTask fetchLocationTask = new FetchLocationTask(cityTableModel);

    Dialogs.progressWorkerDialog(fetchLocationTask)
            .owner(this)
            .title("Fetching locations")
            .stringPainted(true)
            .controls(Controls.builder()
                    .control(Control.builder(fetchLocationTask::cancel)
                            .caption("Cancel")
                            .enabledState(fetchLocationTask.isWorkingObserver()))
                    .build())
            .onException(this::displayFetchException)
            .execute();
  }

  private void displayFetchException(Throwable exception) {
    Dialogs.exceptionDialog()
            .owner(this)
            .title("Unable to fetch location")
            .show(exception);
  }

  private ChartPanel createChartPanel(String title, PieDataset<String> dataset) {
    JFreeChart languagesChart = createPieChart(title, dataset);
    languagesChart.getPlot().setBackgroundPaint(UIManager.getColor("Table.background"));
    languagesChart.setBackgroundPaint(getBackground());
    ChartPanel chartPanel = new ChartPanel(languagesChart);
    chartPanel.getChart().removeLegend();

    return chartPanel;
  }

  private static final class FetchLocationTask implements ProgressTask<Void, String> {

    private final CityTableModel tableModel;
    private final State cancelledState = State.state();

    private FetchLocationTask(CityTableModel tableModel) {
      this.tableModel = tableModel;
    }

    @Override
    public Void perform(ProgressReporter<String> progressReporter) throws Exception {
      tableModel.fetchLocationForSelected(progressReporter, cancelledState);
      return null;
    }

    private void cancel() {
      cancelledState.set(true);
    }

    private StateObserver isWorkingObserver() {
      return cancelledState.getReversedObserver();
    }
  }
}
