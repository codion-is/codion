/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.metadata;

import is.codion.common.db.exception.DatabaseException;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;

public final class MetaDataModel {

  private final Map<String, MetaDataSchema> schemas;
  private final DatabaseMetaData metaData;

  public MetaDataModel(DatabaseMetaData metaData) throws DatabaseException {
    this.metaData = requireNonNull(metaData);
    try {
      this.schemas = discoverSchemas(metaData);
    }
    catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }

  public Collection<MetaDataSchema> schemas() {
    return unmodifiableCollection(schemas.values());
  }

  public void populateSchema(String schemaName, Consumer<String> schemaNotifier) {
    MetaDataSchema schema = schemas.get(requireNonNull(schemaName));
    if (schema == null) {
      throw new IllegalArgumentException("Schema not found: " + schemaName);
    }
    schema.populate(metaData, schemas, schemaNotifier, new HashSet<>());
  }

  private static Map<String, MetaDataSchema> discoverSchemas(DatabaseMetaData metaData) throws SQLException {
    Map<String, MetaDataSchema> schemas = new HashMap<>();
    try (ResultSet resultSet = metaData.getCatalogs()) {
      while (resultSet.next()) {
        String tableCat = resultSet.getString("TABLE_CAT");
        if (tableCat != null) {
          schemas.put(tableCat, new MetaDataSchema(tableCat));
        }
      }
    }
    try (ResultSet resultSet = metaData.getSchemas()) {
      while (resultSet.next()) {
        String tableSchem = resultSet.getString("TABLE_SCHEM");
        if (tableSchem != null) {
          schemas.put(tableSchem, new MetaDataSchema(tableSchem));
        }
      }
    }

    return schemas;
  }
}
