/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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

  TablePacker(Schema schema, DatabaseMetaData metaData, String catalog) {
    this.schema = schema;
    this.metaData = metaData;
    this.catalog = catalog;
  }

  @Override
  public Table fetch(ResultSet resultSet) throws SQLException {
    String tableName = resultSet.getString("TABLE_NAME");
    List<PrimaryKeyColumn> primaryKeyColumns = getPrimaryKeyColumns(schema, metaData, catalog, tableName);
    List<ForeignKeyColumn> foreignKeyColumns = getForeignKeyColumns(schema, metaData, catalog, tableName);
    List<Column> columns = getColumns(schema, metaData, catalog, tableName, primaryKeyColumns, foreignKeyColumns);

    return new Table(schema, tableName, columns, foreignKeyColumns);
  }

  private static List<PrimaryKeyColumn> getPrimaryKeyColumns(Schema schema, DatabaseMetaData metaData,
                                                             String catalog, String tableName) throws SQLException {
    try (ResultSet resultSet = metaData.getPrimaryKeys(catalog, schema.name(), tableName)) {
      return new PrimaryKeyColumnPacker().pack(resultSet);
    }
  }

  private static List<ForeignKeyColumn> getForeignKeyColumns(Schema schema, DatabaseMetaData metaData,
                                                             String catalog, String tableName) throws SQLException {
    try (ResultSet resultSet = metaData.getImportedKeys(catalog, schema.name(), tableName)) {
      return new ForeignKeyColumnPacker().pack(resultSet);
    }
  }

  private static List<Column> getColumns(Schema schema, DatabaseMetaData metaData, String catalog,
                                         String tableName, List<PrimaryKeyColumn> primaryKeyColumns,
                                         List<ForeignKeyColumn> foreignKeyColumns) throws SQLException {
    try (ResultSet resultSet = metaData.getColumns(catalog, schema.name(), tableName, null)) {
      return new ColumnPacker(primaryKeyColumns, foreignKeyColumns).pack(resultSet);
    }
  }
}
