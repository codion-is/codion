/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.framework.domain.property.ColumnProperty;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Factory for standard {@link KeyGenerator} instances.
 */
public final class KeyGenerators {

  private KeyGenerators() {}

  /**
   * Instantiates a primary key generator which fetches the current maximum primary key value and increments
   * it by one prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
   * @param tableName the table name
   * @param columnName the primary key column name
   * @return a incrementing primary key generator
   */
  public static KeyGenerator increment(final String tableName, final String columnName) {
    return new IncrementKeyGenerator(tableName, columnName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values from a sequence prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
   * @param sequenceName the sequence name
   * @return a sequence based primary key generator
   */
  public static KeyGenerator sequence(final String sequenceName) {
    return new SequenceKeyGenerator(sequenceName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values using the given query prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
   * @param query a query for retrieving the primary key value
   * @return a query based primary key generator
   */
  public static KeyGenerator queried(final String query) {
    requireNonNull(query, "query");
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
  public static KeyGenerator automatic(final String valueSource) {
    return new AutomaticKeyGenerator(valueSource);
  }

  /**
   * Instantiates a primary key generator based on an IDENTITY type column.
   * @see Statement#getGeneratedKeys()
   * @return a generated primary key generator
   */
  public static KeyGenerator identity() {
    return new IdentityKeyGenerator();
  }

  private static final class IdentityKeyGenerator implements KeyGenerator {

    @Override
    public boolean isInserted() {
      return false;
    }

    @Override
    public boolean returnGeneratedKeys() {
      return true;
    }

    @Override
    public void afterInsert(final Entity entity, final List<ColumnProperty<?>> primaryKeyProperties,
                            final DatabaseConnection connection, final Statement insertStatement) throws SQLException {
      try (final ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          entity.put((Attribute<Object>) primaryKeyProperties.get(0).getAttribute(), generatedKeys.getObject(1));
        }
      }
    }
  }

  private abstract static class AbstractQueriedKeyGenerator implements KeyGenerator {

    protected final <T> void selectAndPut(final Entity entity, final ColumnProperty<T> keyProperty,
                                          final DatabaseConnection connection) throws SQLException {
      switch (keyProperty.getColumnType()) {
        case Types.INTEGER:
          entity.put((Attribute<Integer>) keyProperty.getAttribute(), connection.selectInteger(getQuery(connection.getDatabase())));
          break;
        case Types.BIGINT:
          entity.put((Attribute<Long>) keyProperty.getAttribute(), connection.selectLong(getQuery(connection.getDatabase())));
          break;
        default:
          throw new SQLException("Queried key generator only implemented for Types.INTEGER and Types.BIGINT datatypes", null, null);
      }
    }

    protected abstract String getQuery(Database database);
  }

  private static final class IncrementKeyGenerator extends AbstractQueriedKeyGenerator {

    private final String query;

    private IncrementKeyGenerator(final String tableName, final String columnName) {
      this.query = "select max(" + requireNonNull(columnName, "columnName") + ") + 1 from " + requireNonNull(tableName, "tableName");
    }

    @Override
    public void beforeInsert(final Entity entity, final List<ColumnProperty<?>> primaryKeyProperties,
                             final DatabaseConnection connection) throws SQLException {
      final ColumnProperty<?> primaryKeyProperty = primaryKeyProperties.get(0);
      if (entity.isNull(primaryKeyProperty.getAttribute())) {
        selectAndPut(entity, primaryKeyProperty, connection);
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
      this.sequenceName = requireNonNull(sequenceName, "sequenceName");
    }

    @Override
    public void beforeInsert(final Entity entity, final List<ColumnProperty<?>> primaryKeyProperties,
                             final DatabaseConnection connection) throws SQLException {
      final ColumnProperty<?> primaryKeyProperty = primaryKeyProperties.get(0);
      if (entity.isNull(primaryKeyProperty.getAttribute())) {
        selectAndPut(entity, primaryKeyProperty, connection);
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
      this.valueSource = requireNonNull(valueSource, "valueSource");
    }

    @Override
    public boolean isInserted() {
      return false;
    }

    @Override
    public void afterInsert(final Entity entity, final List<ColumnProperty<?>> primaryKeyProperties,
                            final DatabaseConnection connection, final Statement insertStatement) throws SQLException {
      selectAndPut(entity, primaryKeyProperties.get(0), connection);
    }

    @Override
    protected String getQuery(final Database database) {
      return database.getAutoIncrementQuery(valueSource);
    }
  }
}
