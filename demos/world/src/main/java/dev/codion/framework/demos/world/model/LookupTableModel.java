package org.jminor.framework.demos.world.model;

import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.ColumnConditionModel.AutomaticWildcard;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.swing.framework.model.SwingEntityTableModel;

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
