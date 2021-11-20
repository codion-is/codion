package is.codion.framework.demos.world.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Entity;
import is.codion.plugin.jasperreports.model.JasperReportsDataSource;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;
import static is.codion.plugin.jasperreports.model.JasperReports.fillReport;

public final class CountryTablePanel extends EntityTablePanel {

  public CountryTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected Controls createPrintControls() {
    final Controls printControls = super.createPrintControls();
    printControls.add(Control.builder(this::viewCountryReport)
            .caption("Country report")
            .build());

    return printControls;
  }

  private void viewCountryReport() throws Exception {
    Dialogs.progressWorkerDialog(this::fillCustomerReport)
            .stringPainted(true)
            .onSuccess(this::viewReport)
            .execute();
  }

  private JasperPrint fillCustomerReport(final ProgressWorker.ProgressReporter<String> progressReporter) throws DatabaseException, ReportException {
    final Map<String, Object> parameters = new HashMap<>();
    parameters.put("CITY_SUBREPORT", classPathReport(CountryTablePanel.class, "city_report.jasper").loadReport());

    final CountryReportDataSource dataSource =
            new CountryReportDataSource(new JasperReportsDataSource<>(getReportCountries().iterator(),
                    new CountryValueProvider(), country -> progressReporter.publish(country.get(Country.NAME))),
                    getTableModel().getConnectionProvider().getConnection());

    return fillReport(classPathReport(CountryTablePanel.class, "country_report.jasper"), dataSource, parameters);
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
    final List<Entity> countries = new ArrayList<>();
    if (getTableModel().getSelectionModel().isSelectionEmpty()) {
      countries.addAll(getTableModel().getItems());
    }
    else {
      countries.addAll(getTableModel().getSelectionModel().getSelectedItems());
    }
    countries.sort(Comparator.comparing(country -> country.get(Country.NAME)));

    return countries;
  }

  public static final class CountryValueProvider implements BiFunction<Entity, JRField, Object> {

    private static final String NAME = "name";
    private static final String CONTINENT = "continent";
    private static final String REGION = "region";
    private static final String SURFACEAREA = "surfacearea";
    private static final String INDEPYEAR = "indipyear";
    private static final String POPULATION = "population";

    @Override
    public Object apply(final Entity entity, final JRField field) {
      switch (field.getName()) {
        case NAME: return entity.get(Country.NAME);
        case CONTINENT: return entity.get(Country.CONTINENT);
        case REGION: return entity.get(Country.REGION);
        case SURFACEAREA: return entity.getAsString(Country.SURFACEAREA);
        case INDEPYEAR: return entity.getAsString(Country.INDEPYEAR);
        case POPULATION: return entity.getAsString(Country.POPULATION);
        default: return "";
      }
    }
  }
}
