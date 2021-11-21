package is.codion.framework.demos.world.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;
import static is.codion.plugin.jasperreports.model.JasperReports.fillReport;
import static java.util.Collections.singletonMap;

public final class CountryTablePanel extends EntityTablePanel {

  public CountryTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected Controls createPrintControls() {
    Controls printControls = super.createPrintControls();
    printControls.add(Control.builder(this::viewCountryReport)
            .caption("Country report")
            .build());

    return printControls;
  }

  private void viewCountryReport() throws Exception {
    Dialogs.progressWorkerDialog(this::fillCustomerReport)
            .indeterminate(false)
            .stringPainted(true)
            .onSuccess(this::viewReport)
            .execute();
  }

  private JasperPrint fillCustomerReport(final ProgressReporter<String> progressReporter) throws DatabaseException, ReportException {
    CountryReportDataSource dataSource = new CountryReportDataSource(getReportCountries(),
            progressReporter, getTableModel().getConnectionProvider().getConnection());

    return fillReport(classPathReport(CountryTablePanel.class, "country_report.jasper"), dataSource, getReportParameters());
  }

  private void viewReport(final JasperPrint customerReport) {
    Dialogs.componentDialog(new JRViewer(customerReport))
            .owner(this)
            .modal(false)
            .title("Country report")
            .preferredSize(new Dimension(800, 600))
            .show();
  }

  private List<Entity> getReportCountries() {
    List<Entity> countries = new ArrayList<>();
    if (getTableModel().getSelectionModel().isSelectionEmpty()) {
      countries.addAll(getTableModel().getItems());
    }
    else {
      countries.addAll(getTableModel().getSelectionModel().getSelectedItems());
    }
    countries.sort(Comparator.comparing(country -> country.get(Country.NAME)));

    return countries;
  }

  private static Map<String, Object> getReportParameters() throws ReportException {
    return new HashMap<>(singletonMap("CITY_SUBREPORT", classPathReport(CountryTablePanel.class, "city_report.jasper").loadReport()));
  }
}
