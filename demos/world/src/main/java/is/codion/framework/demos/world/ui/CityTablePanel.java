package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.common.ui.worker.ProgressWorker;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.awt.Window;
import java.util.List;

import static is.codion.swing.common.ui.Windows.getParentWindow;
import static is.codion.swing.common.ui.control.ControlList.controlListBuilder;
import static is.codion.swing.common.ui.control.Controls.control;
import static is.codion.swing.common.ui.dialog.Dialogs.showExceptionDialog;

public final class CityTablePanel extends EntityTablePanel {

  public CityTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected ControlList getPopupControls(final List<ControlList> additionalPopupControls) {
    return super.getPopupControls(additionalPopupControls)
            .addAt(0, createUpdateLocationControl())
            .addSeparatorAt(1);
  }

  private Control createUpdateLocationControl() {
    return control(this::updateLocation, "Update location",
            getTableModel().getSelectionModel().getSelectionNotEmptyObserver());
  }

  private void updateLocation() {
    new LocationUpdater(getParentWindow(this), (CityTableModel) getTableModel()).execute();
  }

  private static final class LocationUpdater extends ProgressWorker<List<Entity>> {

    private final Window dialogOwner;
    private final CityTableModel cityTableModel;

    private LocationUpdater(final Window dialogOwner, final CityTableModel cityTableModel) {
      super(dialogOwner, "Updating locations", Indeterminate.NO, null,
              controlListBuilder().control(control(cityTableModel::cancelLocationUpdate, "Cancel",
                      cityTableModel.getLocationUpdateCancelledObserver().getReversedObserver())).build());
      this.dialogOwner = dialogOwner;
      this.cityTableModel = cityTableModel;
      setMaximum(cityTableModel.getSelectionModel().getSelectionCount());
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
