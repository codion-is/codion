package is.codion.framework.demos.world.model;

import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

public final class WorldAppModel extends SwingEntityApplicationModel {

  public static final Version VERSION = Version.parsePropertiesFile(WorldAppModel.class, "/version.properties");

  public WorldAppModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    CountryModel countryModel = new CountryModel(connectionProvider);
    SwingEntityModel lookupModel = new SwingEntityModel(new LookupTableModel(connectionProvider));
    ContinentModel continentModel = new ContinentModel(connectionProvider);

    countryModel.tableModel().refresh();
    continentModel.tableModel().refresh();

    addEntityModels(countryModel, lookupModel, continentModel);
  }
}
