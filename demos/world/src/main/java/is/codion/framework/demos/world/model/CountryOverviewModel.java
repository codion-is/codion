package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.swing.framework.model.SwingEntityModel;

public final class CountryOverviewModel extends SwingEntityModel {

  public CountryOverviewModel(EntityConnectionProvider connectionProvider) {
    super(new CountryOverviewTableModel(connectionProvider));
    SwingEntityModel cityModel = new SwingEntityModel(City.TYPE, connectionProvider);
    SwingEntityModel countryLanguageModel = new SwingEntityModel(CountryLanguage.TYPE, connectionProvider);
    addDetailModels(cityModel, countryLanguageModel);
  }
}
