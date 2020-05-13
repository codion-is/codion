package org.jminor.framework.demos.world.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.swing.framework.model.SwingEntityModel;

public final class CountryModel extends SwingEntityModel {

  public CountryModel(final EntityConnectionProvider connectionProvider) {
    super(new CountryEditModel(connectionProvider), new CountryTableModel(connectionProvider));
    SwingEntityModel cityModel = new SwingEntityModel(World.T_CITY, connectionProvider);
    SwingEntityModel countryLanguageModel = new SwingEntityModel(World.T_COUNTRYLANGUAGE, connectionProvider);
    addDetailModels(cityModel, countryLanguageModel);
  }
}
