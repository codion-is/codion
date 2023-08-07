package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.world.domain.WorldImpl;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.base.JRBaseField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static is.codion.framework.db.condition.Condition.attribute;
import static is.codion.framework.db.condition.Condition.where;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class CountryReportDataSourceTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void iterate() throws DatabaseException, JRException {
    try (EntityConnectionProvider connectionProvider = createConnectionProvider()) {
      Value<Integer> progressCounter = Value.value();
      Value<String> publishedValue = Value.value();
      ProgressReporter<String> progressReporter = new ProgressReporter<>() {
        @Override
        public void report(int progress) {
          progressCounter.set(progress);
        }

        @Override
        public void publish(String... chunks) {
          publishedValue.set(chunks[0]);
        }
      };

      EntityConnection connection = connectionProvider.connection();
      List<Entity> countries =
              connection.select(where(attribute(Country.NAME)
                      .in("Denmark", "Iceland"))
                      .selectBuilder()
                      .orderBy(ascending(Country.NAME))
                      .build());
      CountryReportDataSource countryReportDataSource = new CountryReportDataSource(countries, connection, progressReporter);
      assertThrows(IllegalStateException.class, countryReportDataSource::cityDataSource);

      countryReportDataSource.next();
      assertEquals("Denmark", countryReportDataSource.getFieldValue(field(Country.NAME)));
      assertEquals("Europe", countryReportDataSource.getFieldValue(field(Country.CONTINENT)));
      assertEquals("Nordic Countries", countryReportDataSource.getFieldValue(field(Country.REGION)));
      assertEquals(43094d, countryReportDataSource.getFieldValue(field(Country.SURFACEAREA)));
      assertEquals(5330000, countryReportDataSource.getFieldValue(field(Country.POPULATION)));
      assertThrows(IllegalArgumentException.class, () -> countryReportDataSource.getFieldValue(field(City.LOCATION)));

      JRDataSource denmarkCityDataSource = countryReportDataSource.cityDataSource();
      denmarkCityDataSource.next();
      assertEquals("København", denmarkCityDataSource.getFieldValue(field(City.NAME)));
      assertEquals(495699, denmarkCityDataSource.getFieldValue(field(City.POPULATION)));
      assertThrows(IllegalArgumentException.class, () -> denmarkCityDataSource.getFieldValue(field(Country.REGION)));
      denmarkCityDataSource.next();
      assertEquals("Århus", denmarkCityDataSource.getFieldValue(field(City.NAME)));

      countryReportDataSource.next();
      assertEquals("Iceland", countryReportDataSource.getFieldValue(field(Country.NAME)));

      JRDataSource icelandCityDataSource = countryReportDataSource.cityDataSource();
      icelandCityDataSource.next();
      assertEquals("Reykjavík", icelandCityDataSource.getFieldValue(field(City.NAME)));

      assertEquals(100, progressCounter.get());
      assertEquals("Iceland", publishedValue.get());
    }
  }

  private static EntityConnectionProvider createConnectionProvider() {
    return LocalEntityConnectionProvider.builder()
            .domain(new WorldImpl())
            .user(UNIT_TEST_USER)
            .build();
  }

  private static JRField field(Attribute<?> attribute) {
    return new TestField(attribute.name());
  }

  private static final class TestField extends JRBaseField {

    private TestField(String name) {
      this.name = name;
    }
  }
}
