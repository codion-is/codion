package org.jminor.framework.demos.world.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.ColumnConditionModel.AutomaticWildcard;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.util.List;

public final class CountryTableModel extends SwingEntityTableModel {

  private final DefaultPieDataset citiesDataset = new DefaultPieDataset();
  private final DefaultPieDataset languagesDataset = new DefaultPieDataset();

  public CountryTableModel(EntityConnectionProvider connectionProvider) {
    super(World.T_COUNTRY, connectionProvider);
    configureConditionModels();
    bindEvents();
  }

  public PieDataset getCitiesDataset() {
    return citiesDataset;
  }

  public DefaultPieDataset getLanguagesDataset() {
    return languagesDataset;
  }

  private void configureConditionModels() {
    getConditionModel().getPropertyConditionModels().stream().filter(model ->
            model.getColumnIdentifier().isString()).forEach(this::configureConditionModel);
  }

  private void configureConditionModel(ColumnConditionModel model) {
    model.setCaseSensitive(false);
    model.setAutomaticWildcard(AutomaticWildcard.PREFIX_AND_POSTFIX);
  }

  private void bindEvents() {
    getSelectionModel().addSelectedItemsListener(this::updateChartDatasets);
  }

  private void updateChartDatasets(final List<Entity> selectedCountries) {
    citiesDataset.clear();
    languagesDataset.clear();
    try {
      if (!selectedCountries.isEmpty()) {
        final EntityConnection connection = getConnectionProvider().getConnection();

        List<Entity> cities = connection.select(World.T_CITY, World.CITY_COUNTRY_FK, selectedCountries.toArray());
        cities.forEach(city -> citiesDataset.setValue(
                city.getString(World.CITY_NAME),
                city.getInteger(World.CITY_POPULATION)));

        List<Entity> languages = connection.select(World.T_COUNTRYLANGUAGE, World.COUNTRYLANGUAGE_COUNTRY_FK, selectedCountries.toArray());
        languages.forEach(language -> languagesDataset.setValue(
                language.getString(World.COUNTRYLANGUAGE_LANGUAGE),
                language.getInteger(World.COUNTRYLANGUAGE_NO_OF_SPEAKERS)));
      }
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
