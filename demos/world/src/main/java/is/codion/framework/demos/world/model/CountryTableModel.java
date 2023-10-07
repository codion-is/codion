/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.attribute.Condition;
import is.codion.framework.model.EntitySearchConditionModel;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.framework.model.SwingEntityTableModel;

import net.sf.jasperreports.engine.JasperPrint;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static is.codion.plugin.jasperreports.JasperReports.classPathReport;
import static is.codion.plugin.jasperreports.JasperReports.fillReport;
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
    ((EntitySearchConditionModel) conditionModel()
            .attributeModel(Country.CAPITAL_FK))
            .searchModel()
            .condition().set(new CapitalConditionSupplier());
  }

  private final class CapitalConditionSupplier implements Supplier<Condition> {
    @Override
    public Condition get() {
      EntityConnection connection = connectionProvider().connection();
      try {
        return City.ID.in(connection.select(Country.CAPITAL));
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
