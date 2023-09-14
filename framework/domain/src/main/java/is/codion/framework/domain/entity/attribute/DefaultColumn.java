/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.EntityType;

import java.sql.Statement;
import java.util.Collection;
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
    return new DefaultColumnDefinition.DefaultColumnDefinitionBuilder<>(this);
  }

  @Override
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> primaryKey() {
    return (ColumnDefinition.Builder<T, B>) column().primaryKeyIndex(0);
  }

  @Override
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> subquery(String subquery) {
    return new DefaultColumnDefinition.DefaultSubqueryColumnDefinitionBuilder<>(this, subquery);
  }

  @Override
  public <C, B extends ColumnDefinition.Builder<Boolean, B>> ColumnDefinition.Builder<Boolean, B> booleanColumn(Class<C> columnClass,
                                                                                                                C trueValue, C falseValue) {
    if (!isBoolean()) {
      throw new IllegalStateException(this + " is not a boolean column");
    }

    return (ColumnDefinition.Builder<Boolean, B>) new DefaultColumnDefinition.DefaultColumnDefinitionBuilder<>(this)
            .columnClass(columnClass, booleanValueConverter(trueValue, falseValue));
  }

  @Override
  public BlobColumnDefinition.Builder blobColumn() {
    if (!isByteArray()) {
      throw new IllegalStateException(this + " is not a byte array column");
    }

    return new DefaultBlobColumnDefinition.DefaultBlobColumnDefinitionBuilder((Column<byte[]>) this);
  }

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a record was inserted.
   * @param column the column
   * @param <T> the Temporal type to base this column on
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   */
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> auditInsertTimeColumn() {
    if (!isTemporal()) {
      throw new IllegalArgumentException("Column " + this + " is not a temporal column");
    }

    return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>(this, INSERT);
  }

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a record was updated.
   * @param column the column
   * @param <T> the Temporal type to base this column on
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   */
  public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> auditUpdateTimeColumn() {
    if (!isTemporal()) {
      throw new IllegalArgumentException("Column " + this + " is not a temporal column");
    }

    return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>(this, UPDATE);
  }

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who inserted a record.
   * @param column the column
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   * @throws IllegalArgumentException in case this is not a string attribute
   */
  public <B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> auditInsertUserColumn() {
    if (!isString()) {
      throw new IllegalArgumentException("Column " + this + " is not a string column");
    }

    return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>((Column<String>) this, INSERT);
  }

  /**
   * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who updated a record.
   * @param column the column
   * @param <B> the builder type
   * @return a new {@link ColumnDefinition.Builder}
   * @throws IllegalArgumentException in case this is not a string attribute
   */
  public <B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> auditUpdateUserColumn() {
    if (!isString()) {
      throw new IllegalArgumentException("Column " + this + " is not a string column");
    }

    return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>((Column<String>) this, UPDATE);
  }

  @Override
  public ColumnCondition<T> equalTo(T value) {
    return new DefaultColumnConditionBuilder<>(this).equalTo(value);
  }

  @Override
  public ColumnCondition<T> notEqualTo(T value) {
    return new DefaultColumnConditionBuilder<>(this).notEqualTo(value);
  }

  @Override
  public ColumnCondition<String> equalToIgnoreCase(String value) {
    return new DefaultColumnConditionBuilder<>(this).equalToIgnoreCase(value);
  }

  @Override
  public ColumnCondition<String> notEqualToIgnoreCase(String value) {
    return new DefaultColumnConditionBuilder<>(this).notEqualToIgnoreCase(value);
  }

  @Override
  public ColumnCondition<String> like(String value) {
    return new DefaultColumnConditionBuilder<>(this).like(value);
  }

  @Override
  public ColumnCondition<String> notLike(String value) {
    return new DefaultColumnConditionBuilder<>(this).notLike(value);
  }

  @Override
  public ColumnCondition<String> likeIgnoreCase(String value) {
    return new DefaultColumnConditionBuilder<>(this).likeIgnoreCase(value);
  }

  @Override
  public ColumnCondition<String> notLikeIgnoreCase(String value) {
    return new DefaultColumnConditionBuilder<>(this).notLikeIgnoreCase(value);
  }

  @Override
  public ColumnCondition<T> in(T... values) {
    return new DefaultColumnConditionBuilder<>(this).in(values);
  }

  @Override
  public ColumnCondition<T> notIn(T... values) {
    return new DefaultColumnConditionBuilder<>(this).notIn(values);
  }

  @Override
  public ColumnCondition<T> in(Collection<? extends T> values) {
    return new DefaultColumnConditionBuilder<>(this).in(values);
  }

  @Override
  public ColumnCondition<T> notIn(Collection<? extends T> values) {
    return new DefaultColumnConditionBuilder<>(this).notIn(values);
  }

  @Override
  public ColumnCondition<String> inIgnoreCase(String... values) {
    return new DefaultColumnConditionBuilder<>(this).inIgnoreCase(values);
  }

  @Override
  public ColumnCondition<String> notInIgnoreCase(String... values) {
    return new DefaultColumnConditionBuilder<>(this).notInIgnoreCase(values);
  }

  @Override
  public ColumnCondition<String> inIgnoreCase(Collection<String> values) {
    return new DefaultColumnConditionBuilder<>(this).inIgnoreCase(values);
  }

  @Override
  public ColumnCondition<String> notInIgnoreCase(Collection<String> values) {
    return new DefaultColumnConditionBuilder<>(this).notInIgnoreCase(values);
  }

  @Override
  public ColumnCondition<T> lessThan(T value) {
    return new DefaultColumnConditionBuilder<>(this).lessThan(value);
  }

  @Override
  public ColumnCondition<T> lessThanOrEqualTo(T value) {
    return new DefaultColumnConditionBuilder<>(this).lessThanOrEqualTo(value);
  }

  @Override
  public ColumnCondition<T> greaterThan(T value) {
    return new DefaultColumnConditionBuilder<>(this).greaterThan(value);
  }

  @Override
  public ColumnCondition<T> greaterThanOrEqualTo(T value) {
    return new DefaultColumnConditionBuilder<>(this).greaterThanOrEqualTo(value);
  }

  @Override
  public ColumnCondition<T> betweenExclusive(T lowerBound, T upperBound) {
    return new DefaultColumnConditionBuilder<>(this).betweenExclusive(lowerBound, upperBound);
  }

  @Override
  public ColumnCondition<T> between(T lowerBound, T upperBound) {
    return new DefaultColumnConditionBuilder<>(this).between(lowerBound, upperBound);
  }

  @Override
  public ColumnCondition<T> notBetweenExclusive(T lowerBound, T upperBound) {
    return new DefaultColumnConditionBuilder<>(this).notBetweenExclusive(lowerBound, upperBound);
  }

  @Override
  public ColumnCondition<T> notBetween(T lowerBound, T upperBound) {
    return new DefaultColumnConditionBuilder<>(this).notBetween(lowerBound, upperBound);
  }

  @Override
  public ColumnCondition<T> isNull() {
    return new DefaultColumnConditionBuilder<>(this).isNull();
  }

  @Override
  public ColumnCondition<T> isNotNull() {
    return new DefaultColumnConditionBuilder<>(this).isNotNull();
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
