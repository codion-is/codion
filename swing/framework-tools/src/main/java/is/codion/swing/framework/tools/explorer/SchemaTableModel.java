/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.model.table.DefaultColumnFilterModel;
import is.codion.swing.common.model.component.table.AbstractFilteredTableModel;
import is.codion.swing.common.model.component.table.SwingFilteredTableColumnModel;
import is.codion.swing.framework.tools.metadata.Schema;

import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;

final class SchemaTableModel extends AbstractFilteredTableModel<Schema, Integer> {

  static final int SCHEMA = 0;
  static final int POPULATED = 1;

  private final Collection<Schema> schemas;

  SchemaTableModel(Collection<Schema> schemas, SchemaSortModel sortModel) {
    super(new SwingFilteredTableColumnModel<>(createSchemaColumns()), sortModel,
            asList(new DefaultColumnFilterModel<>(0, String.class, '%'),
                    new DefaultColumnFilterModel<>(0, Boolean.class, '%')));
    this.schemas = schemas;
    getSortModel().setSortOrder(0, SortOrder.ASCENDING);
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Schema schema = getItemAt(rowIndex);
    switch (columnIndex) {
      case SCHEMA:
        return schema.getName();
      case POPULATED:
        return schema.isPopulated();
      default:
        throw new IllegalArgumentException("Unknown column: " + columnIndex);
    }
  }

  @Override
  protected Collection<Schema> refreshItems() {
    return schemas;
  }

  private static List<TableColumn> createSchemaColumns() {
    TableColumn schemaColumn = new TableColumn(SchemaTableModel.SCHEMA);
    schemaColumn.setIdentifier(SchemaTableModel.SCHEMA);
    schemaColumn.setHeaderValue("Schema");
    TableColumn populatedColumn = new TableColumn(SchemaTableModel.POPULATED);
    populatedColumn.setIdentifier(SchemaTableModel.POPULATED);
    populatedColumn.setHeaderValue("Populated");

    return asList(schemaColumn, populatedColumn);
  }
}
