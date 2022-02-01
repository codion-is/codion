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
    CountryModel countryModel = new CountryModel(connectionProvider);
    SwingEntityModel lookupModel = new SwingEntityModel(new LookupTableModel(connectionProvider));
    SwingEntityModel continentModel = new ContinentModel(connectionProvider);

    countryModel.refresh();
    continentModel.refresh();

    addEntityModels(countryModel, lookupModel, continentModel);
  }
}
