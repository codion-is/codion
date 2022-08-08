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
    editModel().initializeComboBoxModels(Country.CAPITAL_FK);

    SwingEntityModel cityModel = new SwingEntityModel(new CityTableModel(connectionProvider));
    cityModel.editModel().initializeComboBoxModels(City.COUNTRY_FK);
    SwingEntityModel countryLanguageModel = new SwingEntityModel(new CountryLanguageTableModel(connectionProvider));
    countryLanguageModel.editModel().initializeComboBoxModels(CountryLanguage.COUNTRY_FK);

    addDetailModels(cityModel, countryLanguageModel);

    cityModel.tableModel().addRefreshListener(() ->
            averageCityPopulationValue.set(averageCityPopulation()));
    CountryEditModel countryEditModel = (CountryEditModel) editModel();
    countryEditModel.setAverageCityPopulationObserver(averageCityPopulationValue.observer());
  }

  private Double averageCityPopulation() {
    if (editModel().isEntityNew()) {
      return null;
    }

    SwingEntityTableModel cityTableModel = detailModel(City.TYPE).tableModel();
    Entity country = editModel().entityCopy();

    return Entity.castTo(City.class, cityTableModel.items()).stream()
            .filter(city -> city.isInCountry(country))
            .map(City::population)
            .mapToInt(Integer::valueOf)
            .average()
            .orElse(0d);
  }
}
