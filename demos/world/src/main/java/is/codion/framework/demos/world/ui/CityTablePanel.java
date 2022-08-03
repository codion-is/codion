package is.codion.framework.demos.world.ui;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;

import java.util.List;

final class CityTablePanel extends ChartTablePanel {

  CityTablePanel(CityTableModel tableModel) {
    super(tableModel, tableModel.getChartDataset(), "Cities");
  }

  @Override
  protected Controls createPopupControls(List<Controls> additionalPopupControls) {
    return super.createPopupControls(additionalPopupControls)
            .addAt(0, createFetchLocationControl())
            .addSeparatorAt(1);
  }

  private Control createFetchLocationControl() {
    return Control.builder(this::fetchLocation)
            .caption("Fetch location")
            .enabledState(((CityTableModel) getTableModel()).getCitiesWithoutLocationSelectedObserver())
            .build();
  }

  private void fetchLocation() {
    FetchLocationTask fetchLocationTask = new FetchLocationTask(((CityTableModel) getTableModel()));

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
