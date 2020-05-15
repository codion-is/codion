package dev.codion.framework.demos.world.model;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.demos.world.domain.World;
import dev.codion.swing.framework.model.SwingEntityModel;

public final class CountryModel extends SwingEntityModel {

  public CountryModel(final EntityConnectionProvider connectionProvider) {
    super(new CountryEditModel(connectionProvider), new CountryTableModel(connectionProvider));
    SwingEntityModel cityModel = new SwingEntityModel(World.T_CITY, connectionProvider);
    SwingEntityModel countryLanguageModel = new SwingEntityModel(World.T_COUNTRYLANGUAGE, connectionProvider);
    addDetailModels(cityModel, countryLanguageModel);
  }
}
