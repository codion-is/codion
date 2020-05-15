package dev.codion.framework.demos.world.model;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.swing.framework.model.SwingEntityApplicationModel;
import dev.codion.swing.framework.model.SwingEntityModel;

public final class WorldAppModel extends SwingEntityApplicationModel {

  public WorldAppModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels(connectionProvider);
  }

  private void setupEntityModels(EntityConnectionProvider connectionProvider) {
    SwingEntityModel countryModel = new CountryModel(connectionProvider);
    SwingEntityModel customCountryModel = new CountryCustomModel(connectionProvider);
    SwingEntityModel lookupModel = new SwingEntityModel(new LookupTableModel(connectionProvider));
    SwingEntityModel continentModel = new ContinentModel(connectionProvider);

    addEntityModels(countryModel, lookupModel, continentModel);
  }
}
