package is.codion.framework.demos.world.model;

import is.codion.common.db.report.ReportException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.framework.model.SwingEntityTableModel;

import net.sf.jasperreports.engine.JasperPrint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;
import static is.codion.plugin.jasperreports.model.JasperReports.fillReport;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public final class CountryTableModel extends SwingEntityTableModel {

  private static final String CITY_SUBREPORT_PARAMETER = "CITY_SUBREPORT";
  private static final String COUNTRY_REPORT = "country_report.jasper";
  private static final String CITY_REPORT = "city_report.jasper";

  CountryTableModel(EntityConnectionProvider connectionProvider) {
    super(new CountryEditModel(connectionProvider));
  }

  public JasperPrint fillCountryReport(ProgressReporter<String> progressReporter) throws ReportException {
    CountryReportDataSource dataSource = new CountryReportDataSource(getCountriesForReport(),
            getConnectionProvider().getConnection(), progressReporter);

    return fillReport(classPathReport(CountryTableModel.class, COUNTRY_REPORT), dataSource, getReportParameters());
  }

  private List<Entity> getCountriesForReport() {
    return getSelectionModel().getSelectedItems().stream()
            .sorted(comparing(country -> country.get(World.Country.NAME)))
            .collect(toList());
  }

  private static Map<String, Object> getReportParameters() throws ReportException {
    return new HashMap<>(singletonMap(CITY_SUBREPORT_PARAMETER,
            classPathReport(CityTableModel.class, CITY_REPORT).loadReport()));
  }
}
