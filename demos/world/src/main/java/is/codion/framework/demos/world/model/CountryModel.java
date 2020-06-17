package is.codion.framework.demos.world.model;

import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public final class CountryModel extends SwingEntityModel {

  private final Value<Double> averageCityPopulationValue;

  public CountryModel(EntityConnectionProvider connectionProvider) {
    super(new CountryEditModel(connectionProvider), new CountryTableModel(connectionProvider));
    this.averageCityPopulationValue = ((CountryEditModel) getEditModel()).getAvarageCityPopulationValue();
    SwingEntityModel cityModel = new SwingEntityModel(City.TYPE, connectionProvider);
    SwingEntityModel countryLanguageModel = new SwingEntityModel(CountryLanguage.TYPE, connectionProvider);
    addDetailModels(cityModel, countryLanguageModel);
    bindEvents();
  }

  private void bindEvents() {
    getDetailModel(City.TYPE).getTableModel().addRefreshDoneListener(() ->
            averageCityPopulationValue.set(getAverageCityPopulation()));
  }

  private Double getAverageCityPopulation() {
    SwingEntityTableModel cityTableModel = getDetailModel(City.TYPE).getTableModel();
    Double value = null;
    if (!getEditModel().isEntityNew()) {
      List<City> cities = getEntities().castTo(City.TYPE,
              cityTableModel.getItems()).stream().filter(city ->
              city.isCountry(getEditModel().getEntityCopy())).collect(Collectors.toList());
      OptionalDouble averageCityPopulation =
              cities.stream().map(city -> city.getOptional(City.POPULATION))
                      .filter(Optional::isPresent).map(Optional::get).mapToInt(Integer::valueOf).average();
      value = averageCityPopulation.isPresent() ? averageCityPopulation.getAsDouble() : null;
    }

    return value;
  }
}
