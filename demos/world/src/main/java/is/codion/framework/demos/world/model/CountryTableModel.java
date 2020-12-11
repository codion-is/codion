package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.util.List;

public final class CountryTableModel extends SwingEntityTableModel {

  private final DefaultPieDataset citiesDataset = new DefaultPieDataset();
  private final DefaultPieDataset languagesDataset = new DefaultPieDataset();

  public CountryTableModel(EntityConnectionProvider connectionProvider) {
    super(Country.TYPE, connectionProvider);
    configureConditionModels();
    bindEvents();
  }

  public PieDataset getCitiesDataset() {
    return citiesDataset;
  }

  public PieDataset getLanguagesDataset() {
    return languagesDataset;
  }

  private void configureConditionModels() {
    getTableConditionModel().getConditionModels().stream()
            .filter(model -> model.getColumnIdentifier().getAttribute().isString())
            .forEach(CountryTableModel::configureConditionModel);
  }

  private void bindEvents() {
    getSelectionModel().addSelectedItemsListener(this::refreshChartDatasets);
  }

  private void refreshChartDatasets(List<Entity> selectedCountries) {
    citiesDataset.clear();
    languagesDataset.clear();
    try {
      if (!selectedCountries.isEmpty()) {
        EntityConnection connection = getConnectionProvider().getConnection();
        Entities entities = connection.getEntities();

        List<City> cities = entities.castTo(City.TYPE,
                connection.select(City.COUNTRY_FK, selectedCountries));

        cities.forEach(city -> citiesDataset.setValue(city.name(), city.population()));

        List<CountryLanguage> languages = entities.castTo(CountryLanguage.TYPE,
                connection.select(CountryLanguage.COUNTRY_FK, selectedCountries));

        languages.forEach(language -> languagesDataset.setValue(language.language(), language.noOfSpeakers()));
      }
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private static void configureConditionModel(ColumnConditionModel<?, ?, ?> model) {
    model.setCaseSensitive(false);
    model.setAutomaticWildcard(AutomaticWildcard.PREFIX_AND_POSTFIX);
  }
}
