package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Entity;
import is.codion.plugin.jasperreports.model.JasperReportsDataSource;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRField;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static is.codion.framework.db.condition.Conditions.where;
import static is.codion.framework.domain.entity.OrderBy.orderBy;

public final class CountryReportDataSource extends JasperReportsDataSource<Entity> {

  private final EntityConnection connection;

  CountryReportDataSource(final List<Entity> countries, final EntityConnection connection,
                          final ProgressReporter<String> progressReporter) {
    super(countries.iterator(), new CountryValueProvider(),
            new CountryReportProgressReporter(progressReporter, countries.size()));
    this.connection = connection;
  }

  /* See usage in src/main/reports/country_report.jrxml, subreport element */
  public JRDataSource getCityDataSource() {
    try {
      List<Entity> largestCities = connection.select(where(City.COUNTRY_FK)
              .equalTo(getCurrentItem())
              .toSelectCondition()
              .selectAttributes(City.NAME, City.POPULATION)
              .orderBy(orderBy().descending(City.POPULATION))
              .limit(5));

      return new JasperReportsDataSource<>(largestCities.iterator(), new CityValueProvider());
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private static final class CountryValueProvider implements BiFunction<Entity, JRField, Object> {

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
        case SURFACEAREA: return entity.toString(Country.SURFACEAREA);
        case INDEPYEAR: return entity.toString(Country.INDEPYEAR);
        case POPULATION: return entity.toString(Country.POPULATION);
        default: return "";
      }
    }
  }

  private static final class CityValueProvider implements BiFunction<Entity, JRField, Object> {

    @Override
    public Object apply(final Entity entity, final JRField field) {
      switch (field.getName()) {
        case "name": return entity.get(City.NAME);
        case "population": return entity.toString(City.POPULATION);
        default: return "";
      }
    }
  }

  private static final class CountryReportProgressReporter implements Consumer<Entity> {

    private final AtomicInteger counter = new AtomicInteger();
    private final ProgressReporter<String> progressReporter;
    private final int noOfCountries;

    private CountryReportProgressReporter(final ProgressReporter<String> progressReporter,
                                          final int noOfCountries) {
      this.progressReporter = progressReporter;
      this.noOfCountries = noOfCountries;
    }

    @Override
    public void accept(final Entity country) {
      progressReporter.publish(country.get(Country.NAME));
      progressReporter.setProgress(100 * counter.incrementAndGet() / noOfCountries);
    }
  }
}
