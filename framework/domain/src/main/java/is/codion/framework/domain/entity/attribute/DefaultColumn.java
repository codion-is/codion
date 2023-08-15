/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.EntityType;

import java.sql.Statement;
import java.util.List;
import java.util.Objects;

import static is.codion.framework.domain.entity.attribute.AuditColumn.AuditAction.INSERT;
import static is.codion.framework.domain.entity.attribute.AuditColumn.AuditAction.UPDATE;
import static java.util.Objects.requireNonNull;

final class DefaultColumn<T> extends DefaultAttribute<T> implements Column<T> {

  private static final long serialVersionUID = 1;

  DefaultColumn(String name, Class<T> valueClass, EntityType entityType) {
    super(name, valueClass, entityType);
  }

  @Override
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> column() {
    return column(null);
  }

  @Override
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> column(String caption) {
    return new DefaultColumnDefinition.DefaultColumnDefinitionBuilder<>(this, caption);
  }

  @Override
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> primaryKey() {
    return primaryKey(null);
  }

  @Override
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> primaryKey(String caption) {
    return (ColumnDefinition.Builder<T, B>) column(caption).primaryKeyIndex(0);
  }

  @Override
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> subquery(String subquery) {
    return subquery(null, subquery);
  }

  @Override
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> subquery(String caption, String subquery) {
    return new DefaultColumnDefinition.DefaultSubqueryColumnDefinitionBuilder<>(this, caption, subquery);
  }

  @Override
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> item(List<Item<T>> validItems) {
    return item(null, validItems);
  }

  @Override
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> item(String caption, List<Item<T>> validItems) {
    return new DefaultItemColumnDefinition.DefaultItemColumnDefinitionBuilder<>(this, caption, validItems);
  }

  @Override
  public <C, B extends ColumnDefinition.Builder<Boolean, B>> ColumnDefinition.Builder<Boolean, B> bool(Class<C> columnClass,
                                                                                                       C trueValue, C falseValue) {
    return bool(null, columnClass, trueValue, falseValue);
  }

  @Override
  public <C, B extends ColumnDefinition.Builder<Boolean, B>> ColumnDefinition.Builder<Boolean, B> bool(String caption,
                                                                                                       Class<C> columnClass, C trueValue, C falseValue) {

    return (ColumnDefinition.Builder<Boolean, B>) new DefaultColumnDefinition.DefaultColumnDefinitionBuilder<>(this, caption)
            .columnClass(columnClass, booleanValueConverter(trueValue, falseValue));
  }

  @Override
  public BlobColumnDefinition.Builder blob() {
    return blob(null);
  }

  @Override
  public BlobColumnDefinition.Builder blob(String caption) {
    if (!isByteArray()) {
      throw new IllegalStateException(this + " is not a byte array column");
    }

    return new DefaultBlobColumnDefinition.DefaultBlobColumnDefinitionBuilder((Column<byte[]>) this, caption);
  }

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a record was inserted.
   * @param column the column
   * @param <T> the Temporal type to base this column on
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   */
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> auditInsertTime() {
    return auditInsertTime(null);
  }

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a record was inserted.
   * @param column the column
   * @param caption the caption
   * @param <T> the Temporal type to base this column on
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   */
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> auditInsertTime(String caption) {
    if (!isTemporal()) {
      throw new IllegalArgumentException("Column " + this + " is not a temporal column");
    }

    return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>(this, caption, INSERT);
  }

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a record was updated.
   * @param column the column
   * @param <T> the Temporal type to base this column on
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   */
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> auditUpdateTime() {
    return auditUpdateTime(null);
  }

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a record was updated.
   * @param column the column
   * @param caption the caption
   * @param <T> the Temporal type to base this column on
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   */
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> auditUpdateTime(String caption) {
    if (!isTemporal()) {
      throw new IllegalArgumentException("Column " + this + " is not a temporal column");
    }

    return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>(this, caption, UPDATE);
  }

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who inserted a record.
   * @param column the column
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   * @throws IllegalArgumentException in case this is not a string attribute
   */
  public <B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> auditInsertUser() {
    return auditInsertUser(null);
  }

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who inserted a record.
   * @param column the column
   * @param <B> the builder type
   * @param caption the caption
   * @return a new {@link ColumnDefinition.Builder}
   * @throws IllegalArgumentException in case this is not a string attribute
   */
  public <B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> auditInsertUser(String caption) {
    if (!isString()) {
      throw new IllegalArgumentException("Column " + this + " is not a string column");
    }

    return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>((Column<String>) this, caption, INSERT);
  }

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who updated a record.
   * @param column the column
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   * @throws IllegalArgumentException in case this is not a string attribute
   */
  public <B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> auditUpdateUser() {
    return auditUpdateUser(null);
  }

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who updated a record.
   * @param column the column
   * @param caption the caption
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   * @throws IllegalArgumentException in case this is not a string attribute
   */
  public <B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> auditUpdateUser(String caption) {
    if (!isString()) {
      throw new IllegalArgumentException("Column " + this + " is not a string column");
    }

    return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>((Column<String>) this, caption, UPDATE);
  }

  /**
   * Creates a new {@link Column}, associated with the given entityType.
   * @param entityType the entityType owning this column
   * @param name the column name
   * @param valueClass the class representing the column value type
   * @param <T> the column type
   * @return a new {@link Column}
   */
  public <T> Column<T> column(EntityType entityType, String name, Class<T> valueClass) {
    return new DefaultColumn<>(name, valueClass, entityType);
  }

  private static final class BooleanValueConverter<T> implements ValueConverter<Boolean, T> {

    private final T trueValue;
    private final T falseValue;

    private BooleanValueConverter(T trueValue, T falseValue) {
      this.trueValue = requireNonNull(trueValue);
      this.falseValue = requireNonNull(falseValue);
    }

    @Override
    public Boolean fromColumnValue(T columnValue) {
      if (Objects.equals(trueValue, columnValue)) {
        return true;
      }
      else if (Objects.equals(falseValue, columnValue)) {
        return false;
      }

      return null;
    }

    @Override
    public T toColumnValue(Boolean value, Statement statement) {
      if (value == null) {
        return null;
      }

      if (value) {
        return trueValue;
      }

      return falseValue;
    }
  }

  private <C> ValueConverter<T, C> booleanValueConverter(C trueValue, C falseValue) {
    return (ValueConverter<T, C>) new BooleanValueConverter<>(trueValue, falseValue);
  }
}
