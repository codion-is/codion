package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.framework.model.EntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;

public final class CountryEditModel extends SwingEntityEditModel {

  private final Value<Double> averageCityPopulationValue = Value.value();

  CountryEditModel(EntityConnectionProvider connectionProvider) {
    super(Country.TYPE, connectionProvider);
    initializeComboBoxModels(Country.CAPITAL_FK);
    addEntityListener(country -> averageCityPopulationValue.set(averageCityPopulation(country)));
  }

  @Override
  public EntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey) {
    EntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKey);
    if (foreignKey.equals(Country.CAPITAL_FK)) {
      //only show cities for currently selected country
      addEntityListener(country ->
              comboBoxModel.setIncludeCondition(cityEntity ->
                      cityEntity.castTo(City.class)
                              .isInCountry(country)));
    }

    return comboBoxModel;
  }

  public ValueObserver<Double> averageCityPopulationObserver() {
    return averageCityPopulationValue.observer();
  }

  private Double averageCityPopulation(Entity country) {
    if (country == null) {
      return null;
    }
    try {
      return connectionProvider().connection().execute(Country.AVERAGE_CITY_POPULATION, country.get(Country.CODE));
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
