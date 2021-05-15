package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.worker.ProgressWorker;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.awt.Window;
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
    new LocationUpdater(getParentWindow(this), (CityTableModel) getTableModel()).execute();
  }

  private static final class LocationUpdater extends ProgressWorker<List<Entity>> {

    private final Window dialogOwner;
    private final CityTableModel cityTableModel;

    private LocationUpdater(final Window dialogOwner, final CityTableModel cityTableModel) {
      super(Dialogs.progressDialogBuilder()
              .owner(dialogOwner)
              .title("Updating locations")
              .indeterminate(false)
              .buttonControls(Controls.builder()
                      .control(Control.builder()
                              .command(cityTableModel::cancelLocationUpdate)
                              .name("Cancel")
                              .enabledState(cityTableModel.getLocationUpdateCancelledObserver().getReversedObserver()))
                      .build())
              .build());
      this.dialogOwner = dialogOwner;
      this.cityTableModel = cityTableModel;
      addOnSuccessListener(cityTableModel::replaceEntities);
    }

    @Override
    protected List<Entity> doInBackground() throws Exception {
      return cityTableModel.updateLocationForSelected(this::setProgress);
    }

    @Override
    protected void onException(final Throwable exception) {
      showExceptionDialog(dialogOwner, "Unable to update locations", exception);
    }
  }
}
