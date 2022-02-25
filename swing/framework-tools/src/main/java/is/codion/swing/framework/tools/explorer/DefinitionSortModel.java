/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.swing.common.model.table.AbstractTableSortModel;

final class DefinitionSortModel extends AbstractTableSortModel<DefinitionRow, Integer> {

  DefinitionSortModel() {}

  @Override
  public Class<?> getColumnClass(Integer columnIdentifier) {
    switch (columnIdentifier) {
      case DefinitionTableModel.DOMAIN:
      case DefinitionTableModel.ENTITY:
        return String.class;
      default:
        throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
    }
  }

  @Override
  protected Object getColumnValue(DefinitionRow row, Integer columnIdentifier) {
    switch (columnIdentifier) {
      case DefinitionTableModel.DOMAIN:
        return row.domain.getDomainType().getName();
      case DefinitionTableModel.ENTITY:
        return row.definition.getEntityType().getName();
      default:
        throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
    }
  }
}
