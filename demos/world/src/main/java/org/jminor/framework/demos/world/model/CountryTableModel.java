/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.ColumnConditionModel.AutomaticWildcard;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.util.List;

public final class CountryTableModel extends SwingEntityTableModel {

  private final DefaultPieDataset cityPieDataset = new DefaultPieDataset();
  private final DefaultPieDataset languagePieDataset = new DefaultPieDataset();

  public CountryTableModel(EntityConnectionProvider connectionProvider) {
    super(World.T_COUNTRY, connectionProvider);
    configureConditionModels();
    bindEvents();
  }

  public PieDataset getCityChartDataset() {
    return cityPieDataset;
  }

  public DefaultPieDataset getLanguagePieDataset() {
    return languagePieDataset;
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
    getSelectionModel().addSelectedItemListener(this::updateChartDatasets);
  }

  private void updateChartDatasets(final Entity selectedCountry) {
    cityPieDataset.clear();
    languagePieDataset.clear();
    try {
      if (selectedCountry != null) {
        List<Entity> cities = getConnectionProvider()
                .getConnection().select(World.T_CITY, World.CITY_COUNTRY_FK, selectedCountry);
        cities.forEach(city -> cityPieDataset.setValue(
                city.getString(World.CITY_NAME),
                city.getInteger(World.CITY_POPULATION)));

        List<Entity> languages = getConnectionProvider()
                .getConnection().select(World.T_COUNTRYLANGUAGE, World.COUNTRYLANGUAGE_COUNTRY_FK, selectedCountry);
        languages.forEach(language -> languagePieDataset.setValue(
                language.getString(World.COUNTRYLANGUAGE_LANGUAGE),
                language.getInteger(World.COUNTRYLANGUAGE_NO_OF_SPEAKERS)));
      }
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
