package is.codion.framework.demos.world.model;

import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;

public final class CountryModel extends SwingEntityModel {

  private final Value<Double> averageCityPopulationValue = Value.value();

  CountryModel(EntityConnectionProvider connectionProvider) {
    super(new CountryTableModel(connectionProvider));
    getEditModel().initializeComboBoxModels(Country.CAPITAL_FK);

    SwingEntityModel cityModel = new SwingEntityModel(new CityTableModel(connectionProvider));
    cityModel.getEditModel().initializeComboBoxModels(City.COUNTRY_FK);
    SwingEntityModel countryLanguageModel = new SwingEntityModel(new CountryLanguageTableModel(connectionProvider));
    countryLanguageModel.getEditModel().initializeComboBoxModels(CountryLanguage.COUNTRY_FK);

    addDetailModels(cityModel, countryLanguageModel);

    cityModel.getTableModel().addRefreshListener(() ->
            averageCityPopulationValue.set(getAverageCityPopulation()));
    CountryEditModel countryEditModel = (CountryEditModel) getEditModel();
    countryEditModel.setAverageCityPopulationObserver(averageCityPopulationValue.observer());
  }

  private Double getAverageCityPopulation() {
    if (getEditModel().isEntityNew()) {
      return null;
    }

    SwingEntityTableModel cityTableModel = getDetailModel(City.TYPE).getTableModel();
    Entity country = getEditModel().getEntityCopy();

    return Entity.castTo(City.class, cityTableModel.getItems()).stream()
            .filter(city -> city.isInCountry(country))
            .map(City::population)
            .mapToInt(Integer::valueOf)
            .average()
            .orElse(0d);
  }
}
