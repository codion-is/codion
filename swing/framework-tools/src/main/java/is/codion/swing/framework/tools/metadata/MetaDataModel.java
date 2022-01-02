/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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

import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

public final class MetaDataModel {

  private final Map<String, Schema> schemas;
  private final DatabaseMetaData metaData;

  public MetaDataModel(final DatabaseMetaData metaData) throws DatabaseException {
    this.metaData = requireNonNull(metaData);
    try {
      this.schemas = discoverSchemas(metaData);
    }
    catch (final SQLException e) {
      throw new DatabaseException(e);
    }
  }

  public Collection<Schema> getSchemas() {
    return unmodifiableCollection(schemas.values());
  }

  public void populateSchema(final String schemaName, final EventDataListener<String> schemaNotifier) {
    final Schema schema = schemas.get(requireNonNull(schemaName));
    if (schema == null) {
      throw new IllegalArgumentException("Schema not found: " + schemaName);
    }
    schema.populate(metaData, schemas, schemaNotifier);
  }

  private static Map<String, Schema> discoverSchemas(final DatabaseMetaData metaData) throws SQLException {
    final Map<String, Schema> schemas = new HashMap<>();
    try (final ResultSet resultSet = metaData.getCatalogs()) {
      schemas.putAll(new SchemaPacker("TABLE_CAT").pack(resultSet).stream()
              .collect(toMap(Schema::getName, schema -> schema)));
    }
    try (final ResultSet resultSet = metaData.getSchemas()) {
      schemas.putAll(new SchemaPacker("TABLE_SCHEM").pack(resultSet).stream()
              .collect(toMap(Schema::getName, schema -> schema)));
    }

    return schemas;
  }
}
