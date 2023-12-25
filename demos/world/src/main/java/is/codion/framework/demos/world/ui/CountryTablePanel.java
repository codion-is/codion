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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.world.ui;

import is.codion.common.db.report.ReportException;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.model.CountryTableModel;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import java.awt.Dimension;

final class CountryTablePanel extends EntityTablePanel {

  CountryTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    editableAttributes().remove(Country.CAPITAL_FK);
    control(TableControl.PRINT).set(Control.builder(this::viewCountryReport)
            .name("Country report")
            .enabled(tableModel.selectionModel().selectionNotEmpty())
            .smallIcon(FrameworkIcons.instance().print())
            .build());
  }

  private void viewCountryReport() {
    Dialogs.progressWorkerDialog(this::fillCountryReport)
            .owner(this)
            .maximumProgress(tableModel().selectionModel().selectionCount())
            .stringPainted(true)
            .onResult(this::viewReport)
            .execute();
  }

  private JasperPrint fillCountryReport(ProgressReporter<String> progressReporter) throws ReportException {
    CountryTableModel countryTableModel = tableModel();

    return countryTableModel.fillCountryReport(progressReporter);
  }

  private void viewReport(JasperPrint countryReport) {
    Dialogs.componentDialog(new JRViewer(countryReport))
            .owner(this)
            .modal(false)
            .title("Country report")
            .size(new Dimension(800, 600))
            .show();
  }
}
