package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.worker.ProgressWorker;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.util.List;

import static is.codion.swing.common.ui.Windows.getParentWindow;
import static is.codion.swing.common.ui.dialog.Dialogs.showExceptionDialog;

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

    ProgressWorker.<List<Entity>>builder()
            .dialogOwner(this)
            .dialogTitle("Updating locations")
            .indeterminate(false)
            .buttonControls(Controls.builder()
                    .control(Control.builder()
                            .command(cityTableModel::cancelLocationUpdate)
                            .name("Cancel")
                            .enabledState(cityTableModel.getLocationUpdateCancelledObserver().getReversedObserver()))
                    .build())
            .progressTask(cityTableModel::updateLocationForSelected)
            .onSuccess(cityTableModel::replaceEntities)
            .exceptionHandler(exception -> showExceptionDialog(getParentWindow(this), "Unable to update locations", exception))
            .build().execute();
  }
}
