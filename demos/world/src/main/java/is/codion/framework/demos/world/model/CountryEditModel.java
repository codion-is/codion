package is.codion.framework.demos.world.model;

import is.codion.common.value.ValueObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;

public final class CountryEditModel extends SwingEntityEditModel {

  private ValueObserver<Double> averageCityPopulationObserver;

  CountryEditModel(EntityConnectionProvider connectionProvider) {
    super(Country.TYPE, connectionProvider);
  }

  @Override
  public SwingEntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey) {
    SwingEntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKey);
    if (foreignKey.equals(Country.CAPITAL_FK)) {
      //only show cities for currently selected country
      addEntitySetListener(country ->
              comboBoxModel.setIncludeCondition(cityEntity ->
                      cityEntity.castTo(City.class)
                              .isInCountry(country)));
    }

    return comboBoxModel;
  }

  public ValueObserver<Double> getAvarageCityPopulationValue() {
    return averageCityPopulationObserver;
  }

  public void setAverageCityPopulationObserver(ValueObserver<Double> averageCityPopulationObserver) {
    this.averageCityPopulationObserver = averageCityPopulationObserver;
  }
}
