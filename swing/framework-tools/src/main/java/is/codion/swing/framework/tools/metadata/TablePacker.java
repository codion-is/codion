/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

final class TablePacker implements ResultPacker<Table> {

  private final Schema schema;
  private final DatabaseMetaData metaData;
  private final String catalog;

  TablePacker(final Schema schema, final DatabaseMetaData metaData, final String catalog) {
    this.schema = schema;
    this.metaData = metaData;
    this.catalog = catalog;
  }

  @Override
  public Table fetch(final ResultSet resultSet) throws SQLException {
    String tableName = resultSet.getString("TABLE_NAME");
    List<PrimaryKeyColumn> primaryKeyColumns = getPrimaryKeyColumns(schema, metaData, catalog, tableName);
    List<ForeignKeyColumn> foreignKeyColumns = getForeignKeyColumns(schema, metaData, catalog, tableName);
    List<Column> columns = getColumns(schema, metaData, catalog, tableName, primaryKeyColumns, foreignKeyColumns);

    return new Table(schema, tableName, columns, foreignKeyColumns);
  }

  private static List<PrimaryKeyColumn> getPrimaryKeyColumns(final Schema schema, final DatabaseMetaData metaData,
                                                             final String catalog, final String tableName) throws SQLException {
    try (final ResultSet resultSet = metaData.getPrimaryKeys(catalog, schema.getName(), tableName)) {
      return new PrimaryKeyColumnPacker().pack(resultSet);
    }
  }

  private static List<ForeignKeyColumn> getForeignKeyColumns(final Schema schema, final DatabaseMetaData metaData,
                                                             final String catalog, final String tableName) throws SQLException {
    try (final ResultSet resultSet = metaData.getImportedKeys(catalog, schema.getName(), tableName)) {
      return new ForeignKeyColumnPacker().pack(resultSet);
    }
  }

  private static List<Column> getColumns(final Schema schema, final DatabaseMetaData metaData, final String catalog,
                                         final String tableName, final List<PrimaryKeyColumn> primaryKeyColumns,
                                         final List<ForeignKeyColumn> foreignKeyColumns) throws SQLException {
    try (final ResultSet resultSet = metaData.getColumns(catalog, schema.getName(), tableName, null)) {
      return new ColumnPacker(primaryKeyColumns, foreignKeyColumns).pack(resultSet);
    }
  }
}
