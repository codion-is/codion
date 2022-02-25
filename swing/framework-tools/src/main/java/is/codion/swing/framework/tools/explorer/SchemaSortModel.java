/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.swing.common.model.table.AbstractTableSortModel;
import is.codion.swing.framework.tools.metadata.Schema;

final class SchemaSortModel extends AbstractTableSortModel<Schema, Integer> {

  SchemaSortModel() {}

  @Override
  public Class<?> getColumnClass(Integer columnIdentifier) {
    switch (columnIdentifier) {
      case SchemaTableModel.SCHEMA:
        return String.class;
      case SchemaTableModel.POPULATED:
        return Boolean.class;
      default:
        throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
    }
  }

  @Override
  protected Object getColumnValue(Schema row, Integer columnIdentifier) {
    switch (columnIdentifier) {
      case SchemaTableModel.SCHEMA:
        return row.getName();
      case SchemaTableModel.POPULATED:
        return row.isPopulated();
      default:
        throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
    }
  }
}
