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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static is.codion.framework.db.Select.where;
import static is.codion.framework.db.criteria.Criteria.foreignKey;
import static is.codion.framework.domain.entity.OrderBy.descending;

public final class CountryReportDataSource extends JasperReportsDataSource<Country> {

  private final EntityConnection connection;

  CountryReportDataSource(List<Entity> countries, EntityConnection connection,
                          ProgressReporter<String> progressReporter) {
    super(Entity.castTo(Country.class, countries).iterator(), new CountryValueProvider(),
            new CountryReportProgressReporter(progressReporter, countries.size()));
    this.connection = connection;
  }

  /* See usage in src/main/reports/country_report.jrxml, subreport element */
  public JRDataSource cityDataSource() {
    Country country = currentItem();
    try {
      Collection<City> largestCities = Entity.castTo(City.class,
              connection.select(where(foreignKey(City.COUNTRY_FK).equalTo(country))
                      .attributes(City.NAME, City.POPULATION)
                      .orderBy(descending(City.POPULATION))
                      .limit(5)
                      .build()));

      return new JasperReportsDataSource<>(largestCities.iterator(), new CityValueProvider());
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private static final class CountryValueProvider implements BiFunction<Country, JRField, Object> {

    private static final String NAME = "name";
    private static final String CONTINENT = "continent";
    private static final String REGION = "region";
    private static final String SURFACEAREA = "surfacearea";
    private static final String POPULATION = "population";

    @Override
    public Object apply(Country country, JRField field) {
      switch (field.getName()) {
        case NAME:
          return country.name();
        case CONTINENT:
          return country.continent();
        case REGION:
          return country.region();
        case SURFACEAREA:
          return country.surfacearea();
        case POPULATION:
          return country.population();
        default:
          throw new IllegalArgumentException("Unknown field: " + field.getName());
      }
    }
  }

  private static final class CityValueProvider implements BiFunction<City, JRField, Object> {

    private static final String NAME = "name";
    private static final String POPULATION = "population";

    @Override
    public Object apply(City city, JRField field) {
      switch (field.getName()) {
        case NAME:
          return city.name();
        case POPULATION:
          return city.population();
        default:
          throw new IllegalArgumentException("Unknown field: " + field.getName());
      }
    }
  }

  private static final class CountryReportProgressReporter implements Consumer<Country> {

    private final AtomicInteger counter = new AtomicInteger();
    private final ProgressReporter<String> progressReporter;
    private final int noOfCountries;

    private CountryReportProgressReporter(ProgressReporter<String> progressReporter,
                                          int noOfCountries) {
      this.progressReporter = progressReporter;
      this.noOfCountries = noOfCountries;
    }

    @Override
    public void accept(Country country) {
      progressReporter.publish(country.name());
      progressReporter.report(100 * counter.incrementAndGet() / noOfCountries);
    }
  }
}
