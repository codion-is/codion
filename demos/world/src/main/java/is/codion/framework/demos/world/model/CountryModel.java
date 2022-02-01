package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;

public final class CountryModel extends SwingEntityModel {

  CountryModel(EntityConnectionProvider connectionProvider) {
    super(new CountryEditModel(connectionProvider));
    SwingEntityModel cityModel =
            new SwingEntityModel(new CityTableModel(connectionProvider));
    SwingEntityModel countryLanguageModel =
            new SwingEntityModel(new CountryLanguageTableModel(connectionProvider));
    addDetailModels(cityModel, countryLanguageModel);
    bindEvents();
  }

  private void bindEvents() {
    getDetailModel(City.TYPE).getTableModel().addRefreshSuccessfulListener(() ->
            ((CountryEditModel) getEditModel()).setAverageCityPopulation(getAverageCityPopulation()));
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
