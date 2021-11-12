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

import java.util.List;

public final class CityTablePanel extends EntityTablePanel {

  public CityTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected Controls getPopupControls(List<Controls> additionalPopupControls) {
    return super.getPopupControls(additionalPopupControls)
            .addAt(0, createUpdateLocationControl())
            .addSeparatorAt(1);
  }

  private Control createUpdateLocationControl() {
    return Control.builder(this::updateLocation)
            .caption("Update location")
            .enabledState(getTableModel().getSelectionModel().getSelectionNotEmptyObserver())
            .build();
  }

  private void updateLocation() {
    final UpdateLocationTask updateLocationTask = new UpdateLocationTask((CityTableModel) getTableModel());

    Dialogs.progressWorkerDialogBuilder(updateLocationTask)
            .owner(this)
            .title("Updating locations")
            .stringPainted(true)
            .controls(Controls.builder()
                    .control(Control.builder(updateLocationTask::cancel)
                            .caption("Cancel")
                            .enabledState(updateLocationTask.isWorkingObserver()))
                    .build())
            .onException(this::displayUpdateException)
            .execute();
  }

  private void displayUpdateException(Throwable exception) {
    Dialogs.exceptionDialogBuilder()
            .owner(this)
            .title("Unable to update locations")
            .show(exception);
  }

  private static final class UpdateLocationTask implements ProgressTask<Void, String> {

    private final CityTableModel tableModel;
    private final State cancelledState = State.state();

    private UpdateLocationTask(final CityTableModel tableModel) {
      this.tableModel = tableModel;
    }

    @Override
    public Void perform(ProgressReporter<String> progressReporter) throws Exception {
      tableModel.updateLocationForSelected(progressReporter, cancelledState);
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
