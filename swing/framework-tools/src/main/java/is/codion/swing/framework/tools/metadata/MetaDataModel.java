/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

public final class MetaDataModel {

  private final Map<String, Schema> schemas;
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

  public Collection<Schema> schemas() {
    return unmodifiableCollection(schemas.values());
  }

  public void populateSchema(String schemaName, EventDataListener<String> schemaNotifier) {
    Schema schema = schemas.get(requireNonNull(schemaName));
    if (schema == null) {
      throw new IllegalArgumentException("Schema not found: " + schemaName);
    }
    schema.populate(metaData, schemas, schemaNotifier);
  }

  private static Map<String, Schema> discoverSchemas(DatabaseMetaData metaData) throws SQLException {
    Map<String, Schema> schemas;
    try (ResultSet resultSet = metaData.getCatalogs()) {
      schemas = new HashMap<>(new SchemaPacker("TABLE_CAT").pack(resultSet).stream()
              .collect(toMap(Schema::name, Function.identity())));
    }
    try (ResultSet resultSet = metaData.getSchemas()) {
      schemas.putAll(new SchemaPacker("TABLE_SCHEM").pack(resultSet).stream()
              .collect(toMap(Schema::name, Function.identity())));
    }

    return schemas;
  }
}
