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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;
import static is.codion.plugin.jasperreports.model.JasperReports.fillReport;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public final class CountryTablePanel extends EntityTablePanel {

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
    CountryReportDataSource dataSource = new CountryReportDataSource(getCountriesForReport(),
            getTableModel().getConnectionProvider().getConnection(), progressReporter);

    return fillReport(classPathReport(CountryTablePanel.class, "country_report.jasper"), dataSource, getReportParameters());
  }

  private void viewReport(final JasperPrint countryReport) {
    Dialogs.componentDialog(new JRViewer(countryReport))
            .owner(this)
            .modal(false)
            .title(COUNTRY_REPORT)
            .size(new Dimension(800, 600))
            .show();
  }

  private List<Entity> getCountriesForReport() {
    return getAllOrSelected().stream()
            .sorted(comparing(country -> country.get(Country.NAME)))
            .collect(toList());
  }

  private List<Entity> getAllOrSelected() {
    return getTableModel().getSelectionModel().isSelectionEmpty() ?
            getTableModel().getItems() :
            getTableModel().getSelectionModel().getSelectedItems();
  }

  private static Map<String, Object> getReportParameters() throws ReportException {
    return new HashMap<>(singletonMap("CITY_SUBREPORT",
            classPathReport(CountryTablePanel.class, "city_report.jasper").loadReport()));
  }
}
