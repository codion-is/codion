package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.World.City;
import is.codion.framework.demos.world.domain.World.CountryLanguage;
import is.codion.swing.framework.model.SwingEntityModel;

public final class CountryCustomModel extends SwingEntityModel {

  public CountryCustomModel(EntityConnectionProvider connectionProvider) {
    super(new CountryEditModel(connectionProvider), new CountryTableModel(connectionProvider));
    SwingEntityModel cityModel = new SwingEntityModel(City.TYPE, connectionProvider);
    SwingEntityModel countryLanguageModel = new SwingEntityModel(CountryLanguage.TYPE, connectionProvider);
    addDetailModels(cityModel, countryLanguageModel);
  }
}
