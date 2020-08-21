/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.swing.common.model.table.AbstractTableSortModel;
import is.codion.swing.framework.tools.metadata.Schema;

import javax.swing.table.TableColumn;
import java.util.List;

import static java.util.Arrays.asList;

final class SchemaSortModel extends AbstractTableSortModel<Schema, Integer> {

  SchemaSortModel() {
    super(createSchemaColumns());
  }

  @Override
  public Class<?> getColumnClass(final Integer columnIdentifier) {
    switch (columnIdentifier) {
      case 0:
        return String.class;
      case 1:
        return Boolean.class;
      default:
        throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
    }
  }

  @Override
  protected Comparable<?> getComparable(final Schema row, final Integer columnIdentifier) {
    switch (columnIdentifier) {
      case 0:
        return row.getName();
      case 1:
        return row.isPopulated();
      default:
        throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
    }
  }

  private static List<TableColumn> createSchemaColumns() {
    final TableColumn schemaColumn = new TableColumn(0);
    schemaColumn.setIdentifier(0);
    schemaColumn.setHeaderValue("Schema");
    final TableColumn populatedColumn = new TableColumn(1);
    populatedColumn.setIdentifier(1);
    populatedColumn.setHeaderValue("Populated");

    return asList(schemaColumn, populatedColumn);
  }
}
