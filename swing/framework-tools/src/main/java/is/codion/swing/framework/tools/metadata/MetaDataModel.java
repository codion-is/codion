/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.db.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MetaDataModel {

  private final Map<String, Schema> schemas;

  public MetaDataModel(final Connection connection) throws DatabaseException {
    try {
      this.schemas = analyzeSchemas(connection.getMetaData());
    }
    catch (final SQLException e) {
      throw new DatabaseException(e, e.getMessage());
    }
  }

  public Map<String, Schema> getSchemas() {
    return Collections.unmodifiableMap(schemas);
  }

  private static Map<String, Schema> analyzeSchemas(final DatabaseMetaData metaData) throws SQLException {
    final Map<String, Schema> schemaMap = new HashMap<>();
    ResultSet resultSet = metaData.getSchemas();
    final List<Schema> schemas = new SchemaPacker().pack(resultSet);
    resultSet.close();
    for (final Schema schema : schemas) {
      final List<Table> tables = new ArrayList<>();
      resultSet = metaData.getTables(null, schema.getName(), null, null);
      final String catalog = null;
      while (resultSet.next()) {
        final String tableName = resultSet.getString("TABLE_NAME");
        final ResultSet fkResultSet = metaData.getImportedKeys(catalog, schema.getName(), tableName);
        final List<ForeignKeyColumn> foreignKeyColumns = new ForeignKeyColumnPacker().pack(fkResultSet);
        fkResultSet.close();
        final ResultSet pkResultSet = metaData.getPrimaryKeys(catalog, schema.getName(), tableName);
        final List<PrimaryKeyColumn> primaryKeyColumns = new PrimaryKeyColumnPacker().pack(pkResultSet);
        pkResultSet.close();
        final ResultSet colResultSet = metaData.getColumns(catalog, schema.getName(), tableName, null);
        final List<Column> columns = new ColumnPacker(tableName, primaryKeyColumns, foreignKeyColumns).pack(colResultSet);
        colResultSet.close();
        tables.add(new Table(schema, tableName, columns, foreignKeyColumns));
      }
      resultSet.close();
      schema.setTables(tables);
      schemaMap.put(schema.getName(), schema);
    }
    schemas.forEach(schema -> schema.getTables().values().forEach(table -> table.resolveForeignKeys(schemaMap)));

    return schemaMap;
  }
}
