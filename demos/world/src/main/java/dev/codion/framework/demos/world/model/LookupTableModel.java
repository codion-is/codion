package dev.codion.framework.demos.world.model;

import dev.codion.common.model.table.ColumnConditionModel;
import dev.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.demos.world.domain.World;
import dev.codion.swing.framework.model.SwingEntityTableModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static java.util.Collections.singletonList;

public final class LookupTableModel extends SwingEntityTableModel {

  public LookupTableModel(EntityConnectionProvider connectionProvider) {
    super(World.T_LOOKUP, connectionProvider);
    configureConditionModels();
  }

  public void exportCSV(File file) throws IOException {
    Files.write(file.toPath(), singletonList(getTableDataAsDelimitedString(',')));
  }

  private void configureConditionModels() {
    getConditionModel().getPropertyConditionModels().stream().filter(model ->
            model.getColumnIdentifier().isString()).forEach(this::configureConditionModel);
  }

  private void configureConditionModel(ColumnConditionModel model) {
    model.setCaseSensitive(false);
    model.setAutomaticWildcard(AutomaticWildcard.PREFIX_AND_POSTFIX);
  }
}
