package is.codion.framework.demos.world.ui;

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

  CountryReportDataSource(final List<Entity> countries,
                          final ProgressReporter<String> progressReporter,
                          final EntityConnection connection) {
    super(countries.iterator(), new CountryValueProvider(),
            new CountryReportProgressReporter(progressReporter, countries.size()));
    this.connection = connection;
  }

  public JRDataSource getCityDataSource() {
    try {
        List<Entity> select = connection.select(where(City.COUNTRY_CODE)
                .equalTo(getCurrentItem().get(Country.CODE))
                .toSelectCondition()
                .orderBy(orderBy().ascending(City.NAME)));

        return new JasperReportsDataSource<>(select.iterator(), new CityValueProvider());
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
        case SURFACEAREA: return entity.getAsString(Country.SURFACEAREA);
        case INDEPYEAR: return entity.getAsString(Country.INDEPYEAR);
        case POPULATION: return entity.getAsString(Country.POPULATION);
        default: return "";
      }
    }
  }

  private static final class CityValueProvider implements BiFunction<Entity, JRField, Object> {

    @Override
    public Object apply(final Entity entity, final JRField field) {
      switch (field.getName()) {
        case "name": return entity.get(City.NAME);
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
