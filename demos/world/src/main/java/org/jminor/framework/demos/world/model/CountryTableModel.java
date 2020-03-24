/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.util.List;

public final class CountryTableModel extends SwingEntityTableModel {

  private final DefaultPieDataset cityPieDataset = new DefaultPieDataset();

  public CountryTableModel(EntityConnectionProvider connectionProvider) {
    super(World.T_COUNTRY, connectionProvider);
    bindEvents();
  }

  public PieDataset getCityChartDataset() {
    return cityPieDataset;
  }

  private void bindEvents() {
    getSelectionModel().addSelectedItemListener(this::updateCityPieDataset);
  }

  private void updateCityPieDataset(final Entity selectedCountry) {
    cityPieDataset.clear();
    try {
      if (selectedCountry != null) {
        List<Entity> cities = getConnectionProvider()
                .getConnection().select(World.T_CITY, World.CITY_COUNTRY_FK, selectedCountry);
        cities.forEach(city -> cityPieDataset.setValue(
                city.getString(World.CITY_NAME),
                city.getInteger(World.CITY_POPULATION)));
      }
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
