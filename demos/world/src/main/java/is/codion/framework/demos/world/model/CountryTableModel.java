package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Criteria;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.model.EntitySearchModelConditionModel;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.framework.model.SwingEntityTableModel;

import net.sf.jasperreports.engine.JasperPrint;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static is.codion.framework.db.condition.Condition.attribute;
import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;
import static is.codion.plugin.jasperreports.model.JasperReports.fillReport;
import static java.util.Collections.singletonMap;

public final class CountryTableModel extends SwingEntityTableModel {

  private static final String CITY_SUBREPORT_PARAMETER = "CITY_SUBREPORT";
  private static final String COUNTRY_REPORT = "country_report.jasper";
  private static final String CITY_REPORT = "city_report.jasper";

  CountryTableModel(EntityConnectionProvider connectionProvider) {
    super(new CountryEditModel(connectionProvider));
    configureCapitalConditionModel();
  }

  public JasperPrint fillCountryReport(ProgressReporter<String> progressReporter) throws ReportException {
    CountryReportDataSource dataSource = new CountryReportDataSource(selectionModel().getSelectedItems(),
            connectionProvider().connection(), progressReporter);

    return fillReport(classPathReport(CountryTableModel.class, COUNTRY_REPORT), dataSource, reportParameters());
  }

  private static Map<String, Object> reportParameters() throws ReportException {
    return new HashMap<>(singletonMap(CITY_SUBREPORT_PARAMETER,
            classPathReport(CityTableModel.class, CITY_REPORT).loadReport()));
  }

  private void configureCapitalConditionModel() {
    ((EntitySearchModelConditionModel) conditionModel()
            .attributeModel(Country.CAPITAL_FK))
            .entitySearchModel()
            .setAdditionalCriteriaSupplier(new CapitalConditionSupplier());
  }

  private final class CapitalConditionSupplier implements Supplier<Criteria> {
    @Override
    public Criteria get() {
      EntityConnection connection = connectionProvider().connection();
      try {
        return attribute(City.ID).in(connection.select(Country.CAPITAL));
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
