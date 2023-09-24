/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.ui;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import java.util.List;

final class CityTablePanel extends ChartTablePanel {

  CityTablePanel(CityTableModel tableModel) {
    super(tableModel, tableModel.chartDataset(), "Cities");
  }

  @Override
  protected Controls createPopupMenuControls(List<Controls> additionalPopupMenuControls) {
    return super.createPopupMenuControls(additionalPopupMenuControls)
            .addAt(0, createFetchLocationControl())
            .addSeparatorAt(1);
  }

  private Control createFetchLocationControl() {
    CityTableModel cityTableModel = tableModel();

    return Control.builder(this::fetchLocation)
            .name("Fetch location")
            .enabled(cityTableModel.citiesWithoutLocationSelected())
            .smallIcon(FrameworkIcons.instance().icon(Foundation.MAP))
            .build();
  }

  private void fetchLocation() {
    FetchLocationTask fetchLocationTask = new FetchLocationTask(tableModel());

    Dialogs.progressWorkerDialog(fetchLocationTask)
            .owner(this)
            .title("Fetching locations")
            .stringPainted(true)
            .controls(Controls.builder()
                    .control(Control.builder(fetchLocationTask::cancel)
                            .name("Cancel")
                            .enabled(fetchLocationTask.isWorking()))
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
    private final State cancelled = State.state();

    private FetchLocationTask(CityTableModel tableModel) {
      this.tableModel = tableModel;
    }

    @Override
    public Void perform(ProgressReporter<String> progressReporter) throws Exception {
      tableModel.fetchLocationForSelected(progressReporter, cancelled);
      return null;
    }

    private void cancel() {
      cancelled.set(true);
    }

    private StateObserver isWorking() {
      return cancelled.not();
    }
  }
}
