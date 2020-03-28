package org.jminor.framework.demos.world.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.model.SwingEntityModel;

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
