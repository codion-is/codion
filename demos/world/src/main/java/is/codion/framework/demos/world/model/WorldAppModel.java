package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

public final class WorldAppModel extends SwingEntityApplicationModel {

  public WorldAppModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels(connectionProvider);
  }

  private void setupEntityModels(EntityConnectionProvider connectionProvider) {
    SwingEntityModel countryModel = new CountryModel(connectionProvider);
    SwingEntityModel countryOverviewModel = new CountryOverviewModel(connectionProvider);
    SwingEntityModel lookupModel = new SwingEntityModel(new LookupTableModel(connectionProvider));
    SwingEntityModel continentModel = new ContinentModel(connectionProvider);

    addEntityModels(countryModel, countryOverviewModel, lookupModel, continentModel);
  }
}
