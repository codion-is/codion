/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.framework.domain.property.ColumnProperty;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * Factory for standard {@link org.jminor.framework.domain.Entity.KeyGenerator} instances.
 */
public final class KeyGenerators {

  /**
   * Instantiates a primary key generator which fetches the current maximum primary key value and increments
   * it by one prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
   * @param tableName the table name
   * @param columnName the primary key column name
   * @return a incrementing primary key generator
   */
  public static Entity.KeyGenerator increment(final String tableName, final String columnName) {
    return new IncrementKeyGenerator(tableName, columnName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values from a sequence prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
   * @param sequenceName the sequence name
   * @return a sequence based primary key generator
   */
  public static Entity.KeyGenerator sequence(final String sequenceName) {
    return new SequenceKeyGenerator(sequenceName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values using the given query prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
   * @param query a query for retrieving the primary key value
   * @return a query based primary key generator
   */
  public static Entity.KeyGenerator queried(final String query) {
    return new AbstractQueriedKeyGenerator() {
      @Override
      protected String getQuery(final Database database) {
        return query;
      }
    };
  }

  /**
   * Instantiates a primary key generator which fetches automatically incremented primary key values after insert.
   * @param valueSource the value source, whether a sequence or a table name
   * @return a auto-increment based primary key generator
   */
  public static Entity.KeyGenerator automatic(final String valueSource) {
    return new AutomaticKeyGenerator(valueSource);
  }

  private static abstract class AbstractQueriedKeyGenerator implements Entity.KeyGenerator {

    @Override
    public boolean isInserted() {
      return true;
    }

    protected final void queryAndSet(final Entity entity, final ColumnProperty keyProperty,
                                     final DatabaseConnection connection) throws SQLException {
      final Object value;
      switch (keyProperty.getColumnType()) {
        case Types.INTEGER:
          value = connection.queryInteger(getQuery(connection.getDatabase()));
          break;
        case Types.BIGINT:
          value = connection.queryLong(getQuery(connection.getDatabase()));
          break;
        default:
          throw new SQLException("Queried key generator only implemented for Types.INTEGER and Types.BIGINT datatypes", null, null);
      }
      entity.put(keyProperty, value);
    }

    protected abstract String getQuery(final Database database);
  }

  private static final class IncrementKeyGenerator extends AbstractQueriedKeyGenerator {

    private final String query;

    private IncrementKeyGenerator(final String tableName, final String columnName) {
      this.query = "select max(" + columnName + ") + 1 from " + tableName;
    }

    @Override
    public void beforeInsert(final Entity entity, final DatabaseConnection connection) throws SQLException {
      final ColumnProperty primaryKeyProperty = entity.getPrimaryKeyProperties().get(0);
      if (entity.isNull(primaryKeyProperty)) {
        queryAndSet(entity, primaryKeyProperty, connection);
      }
    }

    @Override
    protected String getQuery(final Database database) {
      return query;
    }
  }

  private static final class SequenceKeyGenerator extends AbstractQueriedKeyGenerator {

    private final String sequenceName;

    private SequenceKeyGenerator(final String sequenceName) {
      this.sequenceName = sequenceName;
    }

    @Override
    public void beforeInsert(final Entity entity, final DatabaseConnection connection) throws SQLException {
      final ColumnProperty primaryKeyProperty = entity.getPrimaryKeyProperties().get(0);
      if (entity.isNull(primaryKeyProperty)) {
        queryAndSet(entity, primaryKeyProperty, connection);
      }
    }

    @Override
    protected String getQuery(final Database database) {
      return database.getSequenceQuery(sequenceName);
    }
  }

  private static final class AutomaticKeyGenerator extends AbstractQueriedKeyGenerator {

    private final String valueSource;

    private AutomaticKeyGenerator(final String valueSource) {
      this.valueSource = valueSource;
    }

    @Override
    public boolean isInserted() {
      return false;
    }

    @Override
    public void afterInsert(final Entity entity, final DatabaseConnection connection,
                            final Statement insertStatement) throws SQLException {
      queryAndSet(entity, entity.getPrimaryKeyProperties().get(0), connection);
    }

    @Override
    protected String getQuery(final Database database) {
      return database.getAutoIncrementQuery(valueSource);
    }
  }
}
