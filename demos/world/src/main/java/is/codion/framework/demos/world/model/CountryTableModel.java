package is.codion.framework.demos.world.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.swing.framework.model.SwingEntityTableModel;

public class CountryTableModel extends SwingEntityTableModel {

  public CountryTableModel(EntityConnectionProvider connectionProvider) {
    super(Country.TYPE, connectionProvider);
    configureConditionModels();
  }

  private void configureConditionModels() {
    getTableConditionModel().getConditionModels().stream()
            .filter(model -> model.getColumnIdentifier().isString())
            .forEach(CountryTableModel::configureConditionModel);
  }

  private static void configureConditionModel(ColumnConditionModel<?, ?> model) {
    model.setCaseSensitive(false);
    model.setAutomaticWildcard(AutomaticWildcard.PREFIX_AND_POSTFIX);
  }
}
