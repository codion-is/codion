/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.world.ui;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.demos.world.domain.api.World.City;
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
    configure().editableAttributes().remove(City.LOCATION);
  }

  @Override
  protected Controls createPopupMenuControls(List<Controls> additionalPopupMenuControls) {
    return super.createPopupMenuControls(additionalPopupMenuControls)
            .addAt(0, createPopulateLocationControl())
            .addSeparatorAt(1);
  }

  private Control createPopulateLocationControl() {
    CityTableModel cityTableModel = tableModel();

    return Control.builder(this::populateLocation)
            .name("Populate location")
            .enabled(cityTableModel.citiesWithoutLocationSelected())
            .smallIcon(FrameworkIcons.instance().icon(Foundation.MAP))
            .build();
  }

  private void populateLocation() {
    PopulateLocationTask populateLocationTask = new PopulateLocationTask(tableModel());

    Dialogs.progressWorkerDialog(populateLocationTask)
            .owner(this)
            .title("Populating locations")
            .stringPainted(true)
            .controls(Controls.builder()
                    .control(Control.builder(populateLocationTask::cancel)
                            .name("Cancel")
                            .enabled(populateLocationTask.isWorking()))
                    .build())
            .onException(this::displayPopulateException)
            .execute();
  }

  private void displayPopulateException(Throwable exception) {
    Dialogs.exceptionDialog()
            .owner(this)
            .title("Unable to populate location")
            .show(exception);
  }

  private static final class PopulateLocationTask implements ProgressTask<Void, String> {

    private final CityTableModel tableModel;
    private final State cancelled = State.state();

    private PopulateLocationTask(CityTableModel tableModel) {
      this.tableModel = tableModel;
    }

    @Override
    public Void execute(ProgressReporter<String> progressReporter) throws Exception {
      tableModel.populateLocationForSelected(progressReporter, cancelled);
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
