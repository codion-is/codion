package is.codion.framework.demos.world.ui;

import is.codion.common.state.State;
import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.worker.ProgressWorker;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.util.List;

public final class CityTablePanel extends EntityTablePanel {

  private final CityTableModel tableModel;
  private final State cancelLocationUpdateState = State.state();

  public CityTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    this.tableModel = (CityTableModel) tableModel;
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
            .enabledState(tableModel.getSelectionModel().getSelectionNotEmptyObserver())
            .build();
  }

  private void updateLocation() {
    ProgressWorker.builder(new UpdateLocationTask())
            .owner(this)
            .title("Updating locations")
            .stringPainted(true)
            .controls(Controls.builder()
                    .control(Control.builder(this::cancelLocationUpdate)
                            .caption("Cancel")
                            .enabledState(cancelLocationUpdateState.getReversedObserver()))
                    .build())
            .onException(this::displayUpdateException)
            .execute();
  }

  private void cancelLocationUpdate() {
    cancelLocationUpdateState.set(true);
  }

  private void displayUpdateException(Throwable exception) {
    Dialogs.exceptionDialogBuilder()
            .owner(this)
            .title("Unable to update locations")
            .show(exception);
  }

  private final class UpdateLocationTask implements ProgressWorker.ProgressTask<Void> {

    @Override
    public Void perform(ProgressWorker.ProgressReporter progressReporter) throws Exception {
      cancelLocationUpdateState.set(false);
      tableModel.updateLocationForSelected(progressReporter, cancelLocationUpdateState);
      return null;
    }
  }
}
