package is.codion.framework.demos.world.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.Lookup;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static java.util.Collections.singletonList;

public final class LookupTableModel extends SwingEntityTableModel {

  public LookupTableModel(EntityConnectionProvider connectionProvider) {
    super(Lookup.TYPE, connectionProvider);
    configureConditionModels();
  }

  public void exportCSV(File file) throws IOException {
    Files.write(file.toPath(), singletonList(getTableDataAsDelimitedString(',')));
  }

  private void configureConditionModels() {
    getTableConditionModel().getConditionModels().values().stream()
            .filter(model -> model.getColumnIdentifier().isString())
            .forEach(LookupTableModel::configureConditionModel);
  }

  private static void configureConditionModel(ColumnConditionModel<?, ?> model) {
    model.setCaseSensitive(false);
    model.setAutomaticWildcard(AutomaticWildcard.PREFIX_AND_POSTFIX);
  }
}
