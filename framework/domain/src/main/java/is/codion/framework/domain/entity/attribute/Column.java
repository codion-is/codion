/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.EntityType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * An {@link Attribute} representing a table column.
 * @param <T> the column value type
 */
public interface Column<T> extends Attribute<T>, ColumnCondition.Builder<T> {

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance.
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   */
  <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> column();

  /**
   * A convenience method for creating a new {@link ColumnDefinition.Builder} instance,
   * with the primary key index set to 0.
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder} with primary key index 0
   */
  <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> primaryKey();

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, based on a subquery.
   * @param subquery the sql query
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   */
  <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> subquery(String subquery);

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance representing a Boolean value.
   * @param <C> the column type
   * @param <B> the builder type
   * @param columnClass the underlying column data type class
   * @param trueValue the value representing 'true' in the underlying column
   * @param falseValue the value representing 'false' in the underlying column
   * @return a new {@link ColumnDefinition.Builder}
   * @throws IllegalStateException in case this columnn is note a boolean column
   */
  <C, B extends ColumnDefinition.Builder<Boolean, B>> ColumnDefinition.Builder<Boolean, B> booleanColumn(Class<C> columnClass,
                                                                                                         C trueValue, C falseValue);

  /**
   * Creates a new {@link BlobColumnDefinition.Builder} instance.
   * @return a new {@link BlobColumnDefinition.Builder}
   * @throws IllegalStateException in case this columnn is note a byte array column
   */
  BlobColumnDefinition.Builder blobColumn();

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a record was inserted.
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   */
  <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> auditInsertTimeColumn();

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a record was updated.
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   */
  <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> auditUpdateTimeColumn();

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who inserted a record.
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   */
  <B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> auditInsertUserColumn();

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who updated a record.
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   */
  <B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> auditUpdateUserColumn();

  /**
   * Creates a new {@link Column}, associated with the given entityType.
   * @param entityType the entityType owning this column
   * @param name the column name
   * @param valueClass the class representing the column value type
   * @param <T> the column type
   * @return a new {@link Column}
   */
  static <T> Column<T> column(EntityType entityType, String name, Class<T> valueClass) {
    return new DefaultColumn<>(name, valueClass, entityType);
  }

  /**
   * Fetches a single value from a result set.
   * @param <C> the type of the column value being fetched
   */
  interface ValueFetcher<C> {

    /**
     * Fetches a single value from a ResultSet
     * @param resultSet the ResultSet
     * @param index the index of the column to fetch
     * @return a single value fetched from the given ResultSet
     * @throws java.sql.SQLException in case of an exception
     */
    C get(ResultSet resultSet, int index) throws SQLException;
  }

  /**
   * Converts to and from SQL values, such as integers being used to represent booleans in a database.
   * @param <T> the type of the value
   * @param <C> the type of the underlying column
   */
  interface ValueConverter<T, C> {

    /**
     * Translates the given value into a sql value, usually this is not required
     * but for certain types this may be necessary, such as boolean values where
     * the values are represented by a non-boolean data type in the underlying database
     * @param value the value to translate
     * @param statement the statement using the value, may be null
     * @return the sql value used to represent the given value
     * @throws SQLException in case of an exception
     */
    C toColumnValue(T value, Statement statement) throws SQLException;

    /**
     * Translates the given sql column value into a column value.
     * @param columnValue the sql value to translate from
     * @return the value of sql {@code columnValue}
     * @throws SQLException in case of an exception
     */
    T fromColumnValue(C columnValue) throws SQLException;
  }
}
