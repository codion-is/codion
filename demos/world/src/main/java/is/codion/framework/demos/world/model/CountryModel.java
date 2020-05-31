package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.World;
import is.codion.swing.framework.model.SwingEntityModel;

public final class CountryModel extends SwingEntityModel {

  public CountryModel(EntityConnectionProvider connectionProvider) {
    super(new CountryEditModel(connectionProvider), new CountryTableModel(connectionProvider));
    SwingEntityModel cityModel = new SwingEntityModel(World.T_CITY, connectionProvider);
    SwingEntityModel countryLanguageModel = new SwingEntityModel(World.T_COUNTRYLANGUAGE, connectionProvider);
    addDetailModels(cityModel, countryLanguageModel);
  }
}
