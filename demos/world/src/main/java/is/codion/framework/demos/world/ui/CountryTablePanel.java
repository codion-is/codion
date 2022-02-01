package is.codion.framework.demos.world.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
import is.codion.framework.demos.world.model.CountryTableModel;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import java.awt.Dimension;

final class CountryTablePanel extends EntityTablePanel {

  private static final String COUNTRY_REPORT = "Country report";

  public CountryTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected Controls createPrintControls() {
    return super.createPrintControls()
            .add(Control.builder(this::viewCountryReport)
                    .caption(COUNTRY_REPORT)
                    .build());
  }

  private void viewCountryReport() throws Exception {
    Dialogs.progressWorkerDialog(this::fillCountryReport)
            .indeterminate(false)
            .stringPainted(true)
            .onSuccess(this::viewReport)
            .execute();
  }

  private JasperPrint fillCountryReport(final ProgressReporter<String> progressReporter) throws DatabaseException, ReportException {
    return ((CountryTableModel) getTableModel()).fillCountryReport(progressReporter);
  }

  private void viewReport(final JasperPrint countryReport) {
    Dialogs.componentDialog(new JRViewer(countryReport))
            .owner(this)
            .modal(false)
            .title(COUNTRY_REPORT)
            .size(new Dimension(800, 600))
            .show();
  }
}
