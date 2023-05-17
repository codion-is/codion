package is.codion.framework.demos.world.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
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
    setControl(ControlCode.PRINT, Control.builder(this::viewCountryReport)
            .caption("Country report")
            .enabledState(tableModel.selectionModel().selectionNotEmptyObserver())
            .smallIcon(FrameworkIcons.instance().print())
            .build());
  }

  private void viewCountryReport() throws Exception {
    Dialogs.progressWorkerDialog(this::fillCountryReport)
            .owner(this)
            .indeterminate(false)
            .stringPainted(true)
            .onResult(this::viewReport)
            .execute();
  }

  private JasperPrint fillCountryReport(ProgressReporter<String> progressReporter) throws DatabaseException, ReportException {
    return ((CountryTableModel) tableModel()).fillCountryReport(progressReporter);
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
