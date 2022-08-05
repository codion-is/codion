/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.model.table.DefaultColumnFilterModel;
import is.codion.swing.common.model.component.table.DefaultFilteredTableModel;
import is.codion.swing.framework.tools.metadata.Schema;

import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;

final class SchemaTableModel extends DefaultFilteredTableModel<Schema, Integer> {

  static final int SCHEMA = 0;
  static final int POPULATED = 1;

  private final Collection<Schema> schemas;

  SchemaTableModel(Collection<Schema> schemas) {
    super(createSchemaColumns(), new SchemaColumnValueProvider(),
            asList(new DefaultColumnFilterModel<>(0, String.class, '%'),
                    new DefaultColumnFilterModel<>(0, Boolean.class, '%')));
    this.schemas = schemas;
    sortModel().setSortOrder(0, SortOrder.ASCENDING);
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

  private static final class SchemaColumnValueProvider implements ColumnValueProvider<Schema, Integer> {

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
    public Object getValue(Schema row, Integer columnIdentifier) {
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
}
