/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.swing.common.model.table.AbstractTableSortModel;

import javax.swing.table.TableColumn;
import java.util.List;

import static java.util.Arrays.asList;

final class DefinitionSortModel extends AbstractTableSortModel<DefinitionRow, Integer> {

  DefinitionSortModel() {
    super(createDefinitionColumns());
  }

  @Override
  public Class<?> getColumnClass(final Integer columnIdentifier) {
    switch (columnIdentifier) {
      case 0:
      case 1:
        return String.class;
      default:
        throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
    }
  }

  @Override
  protected Comparable<?> getComparable(final DefinitionRow row, final Integer columnIdentifier) {
    switch (columnIdentifier) {
      case 0:
        return row.domain.getDomainType().getName();
      case 1:
        return row.definition.getEntityType().getName();
      default:
        throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
    }
  }

  private static List<TableColumn> createDefinitionColumns() {
    final TableColumn domainColumn = new TableColumn(0);
    domainColumn.setIdentifier(0);
    domainColumn.setHeaderValue("Domain");
    final TableColumn entityTypeColumn = new TableColumn(1);
    entityTypeColumn.setIdentifier(1);
    entityTypeColumn.setHeaderValue("Entity");

    return asList(domainColumn, entityTypeColumn);
  }
}
