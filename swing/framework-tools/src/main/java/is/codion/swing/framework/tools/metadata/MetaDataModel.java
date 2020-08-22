/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableCollection;

public final class MetaDataModel {

  private final Map<String, Schema> schemas;
  private final DatabaseMetaData metaData;

  public MetaDataModel(final DatabaseMetaData metaData) throws DatabaseException {
    this.metaData = metaData;
    try {
      this.schemas = discoverSchemas(metaData);
    }
    catch (final SQLException e) {
      throw new DatabaseException(e, e.getMessage());
    }
  }

  public Collection<Schema> getSchemas() {
    return unmodifiableCollection(schemas.values());
  }

  public void populateSchema(final String schemaName, final EventDataListener<String> schemaNotifier) {
    final Schema schema = schemas.get(schemaName);
    if (schema == null) {
      throw new IllegalArgumentException("Schema not found: " + schemaName);
    }
    schema.populate(metaData, schemas, schemaNotifier);
  }

  public void resolveForeignKeys() {
    schemas.values().forEach(schema -> schema.getTables().values().forEach(table -> table.resolveForeignKeys(schemas)));
  }

  private static Map<String, Schema> discoverSchemas(final DatabaseMetaData metaData) throws SQLException {
    try (final ResultSet resultSet = metaData.getSchemas()) {
      return new SchemaPacker().pack(resultSet).stream().collect(Collectors.toMap(Schema::getName, schema -> schema));
    }
  }
}
