package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.util.List;

public final class CountryOverviewTableModel extends SwingEntityTableModel {

  private final DefaultPieDataset<String> citiesDataset = new DefaultPieDataset<>();
  private final DefaultPieDataset<String> languagesDataset = new DefaultPieDataset<>();

  public CountryOverviewTableModel(EntityConnectionProvider connectionProvider) {
    super(Country.TYPE, connectionProvider);
    bindEvents();
  }

  public PieDataset<String> getCitiesDataset() {
    return citiesDataset;
  }

  public PieDataset<String> getLanguagesDataset() {
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

        List<City> cities = Entity.castTo(City.class,
                connection.select(City.COUNTRY_FK, selectedCountries));

        cities.forEach(city -> citiesDataset.setValue(city.name(), city.population()));

        List<CountryLanguage> languages = Entity.castTo(CountryLanguage.class,
                connection.select(CountryLanguage.COUNTRY_FK, selectedCountries));

        languages.forEach(language -> languagesDataset.setValue(language.language(), language.noOfSpeakers()));
      }
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
