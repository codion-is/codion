package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.util.List;

public final class CountryOverviewTableModel extends CountryTableModel {

  private final DefaultPieDataset citiesDataset = new DefaultPieDataset();
  private final DefaultPieDataset languagesDataset = new DefaultPieDataset();

  public CountryOverviewTableModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    bindEvents();
  }

  public PieDataset getCitiesDataset() {
    return citiesDataset;
  }

  public PieDataset getLanguagesDataset() {
    return languagesDataset;
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
}
