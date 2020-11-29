package is.codion.framework.demos.world.model;

import is.codion.common.value.Value;
import is.codion.common.value.Values;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;

public final class CountryEditModel extends SwingEntityEditModel {

  private final Value<Double> averageCityPopulationValue = Values.value();

  public CountryEditModel(EntityConnectionProvider connectionProvider) {
    super(Country.TYPE, connectionProvider);
  }

  @Override
  public SwingEntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey) {
    SwingEntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKey);
    if (foreignKey.equals(Country.CAPITAL_FK)) {
      //only show cities for currently selected country
      addEntitySetListener(country ->
              comboBoxModel.setIncludeCondition(cityEntity ->
                      getEntities().castTo(City.TYPE, cityEntity).isInCountry(country)));
    }

    return comboBoxModel;
  }

  public void setAverageCityPopulation(Double value) {
    averageCityPopulationValue.set(value);
  }

  public Value<Double> getAvarageCityPopulationValue() {
    return averageCityPopulationValue;
  }
}
