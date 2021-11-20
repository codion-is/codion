package is.codion.framework.demos.world.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Entity;
import is.codion.plugin.jasperreports.model.JasperReportsDataSource;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static is.codion.framework.db.condition.Conditions.where;
import static is.codion.framework.domain.entity.OrderBy.orderBy;

public final class CountryReportDataSource implements JRDataSource {

  private final JasperReportsDataSource<Entity> dataSource;
  private final EntityConnection connection;

  CountryReportDataSource(final JasperReportsDataSource<Entity> dataSource,
                          final EntityConnection connection) {
    this.dataSource = dataSource;
    this.connection = connection;
  }

  @Override
  public boolean next() throws JRException {
    return dataSource.next();
  }

  @Override
  public Object getFieldValue(final JRField field) throws JRException {
    return dataSource.getFieldValue(field);
  }

  public JRDataSource getCityDataSource() {
    try {
      final Optional<Entity> entity = dataSource.currentItem();
      if (entity.isPresent()) {
        final List<Entity> select = connection.select(where(City.COUNTRY_CODE).equalTo(entity.get().get(Country.CODE))
                .toSelectCondition()
                .orderBy(orderBy().ascending(City.NAME)));

        return new JasperReportsDataSource<>(select.iterator(), new CityValueProvider());
      }

      return null;
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
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
}
