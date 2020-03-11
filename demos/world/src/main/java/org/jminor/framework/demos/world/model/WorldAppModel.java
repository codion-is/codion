package org.jminor.framework.demos.world.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.model.SwingEntityModel;

public final class WorldAppModel extends SwingEntityApplicationModel {

  public WorldAppModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels(connectionProvider);
  }

  private void setupEntityModels(EntityConnectionProvider connectionProvider) {
    SwingEntityModel countryModel = new SwingEntityModel(new CountryEditModel(connectionProvider));
    SwingEntityModel cityModel = new SwingEntityModel(World.T_CITY, connectionProvider);
    SwingEntityModel countryLanguageModel = new SwingEntityModel(World.T_COUNTRYLANGUAGE, connectionProvider);
    countryModel.addDetailModels(cityModel, countryLanguageModel);

    SwingEntityModel lookupModel = new SwingEntityModel(new LookupTableModel(connectionProvider));

    addEntityModels(countryModel, lookupModel);
  }
}
