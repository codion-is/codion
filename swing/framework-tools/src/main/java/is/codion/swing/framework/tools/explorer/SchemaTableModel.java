/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.common.model.table.SortingDirective;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.framework.tools.metadata.Schema;

import java.util.ArrayList;
import java.util.Collection;

import static java.util.Arrays.asList;

final class SchemaTableModel extends AbstractFilteredTableModel<Schema, Integer> {

  private final Collection<Schema> schemas;

  SchemaTableModel(final Collection<Schema> schemas) {
    super(new SchemaSortModel(),
            asList(new DefaultColumnConditionModel<>(0, String.class, "%"),
                    new DefaultColumnConditionModel<>(0, Boolean.class, "%")));
    this.schemas = schemas;
    getSortModel().setSortingDirective(0, SortingDirective.ASCENDING);
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final Schema schema = getItemAt(rowIndex);
    switch (columnIndex) {
      case 0:
        return schema.getName();
      case 1:
        return schema.isPopulated();
      default:
        throw new IllegalArgumentException("Unknown column: " + columnIndex);
    }
  }

  @Override
  protected void refreshModel() {
    clear();
    addItemsSorted(new ArrayList<>(schemas));
  }
}
