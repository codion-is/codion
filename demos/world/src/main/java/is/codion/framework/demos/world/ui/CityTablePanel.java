package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.worker.ProgressWorker;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.util.List;

public final class CityTablePanel extends EntityTablePanel {

  public CityTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected Controls getPopupControls(final List<Controls> additionalPopupControls) {
    return super.getPopupControls(additionalPopupControls)
            .addAt(0, createUpdateLocationControl())
            .addSeparatorAt(1);
  }

  private Control createUpdateLocationControl() {
    return Control.builder()
            .command(this::updateLocation)
            .name("Update location")
            .enabledState(getTableModel().getSelectionModel().getSelectionNotEmptyObserver())
            .build();
  }

  private void updateLocation() {
    final CityTableModel cityTableModel = (CityTableModel) getTableModel();

    ProgressWorker.builder(cityTableModel::updateLocationForSelected)
            .owner(this)
            .title("Updating locations")
            .indeterminate(false)
            .stringPainted(true)
            .buttonControls(Controls.builder()
                    .control(Control.builder()
                            .command(cityTableModel::cancelLocationUpdate)
                            .name("Cancel")
                            .enabledState(cityTableModel.getLocationUpdateCancelledObserver().getReversedObserver()))
                    .build())
            .onSuccess(cityTableModel::replaceEntities)
            .onException(this::displayUpdateException)
            .execute();
  }

  private void displayUpdateException(final Throwable exception) {
    Dialogs.exceptionDialogBuilder()
            .owner(this)
            .title("Unable to update locations")
            .show(exception);
  }
}
