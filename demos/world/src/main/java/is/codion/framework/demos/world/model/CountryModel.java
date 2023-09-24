/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;

public final class CountryModel extends SwingEntityModel {

  CountryModel(EntityConnectionProvider connectionProvider) {
    super(new CountryTableModel(connectionProvider));
    SwingEntityModel cityModel = new SwingEntityModel(new CityTableModel(connectionProvider));
    SwingEntityModel countryLanguageModel = new SwingEntityModel(new CountryLanguageTableModel(connectionProvider));
    addDetailModels(cityModel, countryLanguageModel);
  }
}
