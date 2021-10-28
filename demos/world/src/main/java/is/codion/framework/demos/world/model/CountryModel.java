package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public final class CountryModel extends SwingEntityModel {

  public CountryModel(EntityConnectionProvider connectionProvider) {
    super(new CountryEditModel(connectionProvider), new CountryTableModel(connectionProvider));
    SwingEntityModel cityModel = new SwingEntityModel(new CityTableModel(connectionProvider));
    SwingEntityModel countryLanguageModel = new SwingEntityModel(CountryLanguage.TYPE, connectionProvider);
    addDetailModels(cityModel, countryLanguageModel);
    bindEvents();
  }

  private void bindEvents() {
    getDetailModel(City.TYPE).getTableModel().addRefreshDoneListener(() ->
            ((CountryEditModel) getEditModel()).setAverageCityPopulation(getAverageCityPopulation()));
  }

  private Double getAverageCityPopulation() {
    SwingEntityTableModel cityTableModel = getDetailModel(City.TYPE).getTableModel();
    if (!getEditModel().isEntityNew()) {
      Entity country = getEditModel().getEntityCopy();

      List<City> cities = Entity.castTo(City.class, cityTableModel.getItems()).stream()
              .filter(city -> city.isInCountry(country))
              .collect(Collectors.toList());

      OptionalDouble averageCityPopulation = cities.stream()
              .map(city -> city.getOptional(City.POPULATION))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .mapToInt(Integer::valueOf)
              .average();

      return averageCityPopulation.isPresent() ? averageCityPopulation.getAsDouble() : null;
    }

    return null;
  }
}
