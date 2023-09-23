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
  public Column.ColumnDefiner<T> define() {
    return new DefaultColumnDefiner<>(this);
  }

  @Override
  public ColumnCondition<T> equalTo(T value) {
    return new DefaultColumnConditionFactory<>(this).equalTo(value);
  }

  @Override
  public ColumnCondition<T> notEqualTo(T value) {
    return new DefaultColumnConditionFactory<>(this).notEqualTo(value);
  }

  @Override
  public ColumnCondition<String> equalToIgnoreCase(String value) {
    return new DefaultColumnConditionFactory<>(this).equalToIgnoreCase(value);
  }

  @Override
  public ColumnCondition<String> notEqualToIgnoreCase(String value) {
    return new DefaultColumnConditionFactory<>(this).notEqualToIgnoreCase(value);
  }

  @Override
  public ColumnCondition<String> like(String value) {
    return new DefaultColumnConditionFactory<>(this).like(value);
  }

  @Override
  public ColumnCondition<String> notLike(String value) {
    return new DefaultColumnConditionFactory<>(this).notLike(value);
  }

  @Override
  public ColumnCondition<String> likeIgnoreCase(String value) {
    return new DefaultColumnConditionFactory<>(this).likeIgnoreCase(value);
  }

  @Override
  public ColumnCondition<String> notLikeIgnoreCase(String value) {
    return new DefaultColumnConditionFactory<>(this).notLikeIgnoreCase(value);
  }

  @Override
  public ColumnCondition<T> in(T... values) {
    return new DefaultColumnConditionFactory<>(this).in(values);
  }

  @Override
  public ColumnCondition<T> notIn(T... values) {
    return new DefaultColumnConditionFactory<>(this).notIn(values);
  }

  @Override
  public ColumnCondition<T> in(Collection<? extends T> values) {
    return new DefaultColumnConditionFactory<>(this).in(values);
  }

  @Override
  public ColumnCondition<T> notIn(Collection<? extends T> values) {
    return new DefaultColumnConditionFactory<>(this).notIn(values);
  }

  @Override
  public ColumnCondition<String> inIgnoreCase(String... values) {
    return new DefaultColumnConditionFactory<>(this).inIgnoreCase(values);
  }

  @Override
  public ColumnCondition<String> notInIgnoreCase(String... values) {
    return new DefaultColumnConditionFactory<>(this).notInIgnoreCase(values);
  }

  @Override
  public ColumnCondition<String> inIgnoreCase(Collection<String> values) {
    return new DefaultColumnConditionFactory<>(this).inIgnoreCase(values);
  }

  @Override
  public ColumnCondition<String> notInIgnoreCase(Collection<String> values) {
    return new DefaultColumnConditionFactory<>(this).notInIgnoreCase(values);
  }

  @Override
  public ColumnCondition<T> lessThan(T value) {
    return new DefaultColumnConditionFactory<>(this).lessThan(value);
  }

  @Override
  public ColumnCondition<T> lessThanOrEqualTo(T value) {
    return new DefaultColumnConditionFactory<>(this).lessThanOrEqualTo(value);
  }

  @Override
  public ColumnCondition<T> greaterThan(T value) {
    return new DefaultColumnConditionFactory<>(this).greaterThan(value);
  }

  @Override
  public ColumnCondition<T> greaterThanOrEqualTo(T value) {
    return new DefaultColumnConditionFactory<>(this).greaterThanOrEqualTo(value);
  }

  @Override
  public ColumnCondition<T> betweenExclusive(T lowerBound, T upperBound) {
    return new DefaultColumnConditionFactory<>(this).betweenExclusive(lowerBound, upperBound);
  }

  @Override
  public ColumnCondition<T> between(T lowerBound, T upperBound) {
    return new DefaultColumnConditionFactory<>(this).between(lowerBound, upperBound);
  }

  @Override
  public ColumnCondition<T> notBetweenExclusive(T lowerBound, T upperBound) {
    return new DefaultColumnConditionFactory<>(this).notBetweenExclusive(lowerBound, upperBound);
  }

  @Override
  public ColumnCondition<T> notBetween(T lowerBound, T upperBound) {
    return new DefaultColumnConditionFactory<>(this).notBetween(lowerBound, upperBound);
  }

  @Override
  public ColumnCondition<T> isNull() {
    return new DefaultColumnConditionFactory<>(this).isNull();
  }

  @Override
  public ColumnCondition<T> isNotNull() {
    return new DefaultColumnConditionFactory<>(this).isNotNull();
  }

  static final class BooleanValueConverter<T> implements ValueConverter<Boolean, T> {

    private final T trueValue;
    private final T falseValue;

    BooleanValueConverter(T trueValue, T falseValue) {
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

  private final class DefaultColumnDefiner<T> extends DefaultAttributeDefiner<T> implements Column.ColumnDefiner<T> {

    private static final String COLUMN_CAPTION = "Column";

    private final Column<T> column;

    private DefaultColumnDefiner(Column<T> column) {
      super(column);
      this.column = column;
    }

    @Override
    public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> column() {
      return new DefaultColumnDefinition.DefaultColumnDefinitionBuilder<>(column);
    }

    @Override
    public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> primaryKey() {
      return (ColumnDefinition.Builder<T, B>) column().primaryKeyIndex(0);
    }

    @Override
    public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> subquery(String subquery) {
      return new DefaultColumnDefinition.DefaultSubqueryColumnDefinitionBuilder<>(column, subquery);
    }

    @Override
    public <C, B extends ColumnDefinition.Builder<Boolean, B>> ColumnDefinition.Builder<Boolean, B> booleanColumn(Class<C> columnClass,
                                                                                                                  C trueValue, C falseValue) {
      if (!type().isBoolean()) {
        throw new IllegalStateException(column + " is not a boolean column");
      }

      return (ColumnDefinition.Builder<Boolean, B>) new DefaultColumnDefinition.DefaultColumnDefinitionBuilder<>(column)
              .columnClass(columnClass, (ValueConverter<T, C>) new BooleanValueConverter<>(trueValue, falseValue));
    }

    @Override
    public BlobColumnDefinition.Builder blobColumn() {
      if (!type().isByteArray()) {
        throw new IllegalStateException(column + " is not a byte array column");
      }

      return new DefaultBlobColumnDefinition.DefaultBlobColumnDefinitionBuilder((Column<byte[]>) column);
    }

    /**
     * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a row was inserted.
     * @param column the column
     * @param <T> the Temporal type to base column column on
     * @param <B> the builder type
     * @return a new {@link ColumnDefinition.Builder}
     */
    public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> auditInsertTimeColumn() {
      if (!type().isTemporal()) {
        throw new IllegalArgumentException(COLUMN_CAPTION + " " + column + " is not a temporal column");
      }

      return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>(column, INSERT);
    }

    /**
     * Creates a new {@link ColumnDefinition.Builder} instance, representing the time a row was updated.
     * @param column the column
     * @param <T> the Temporal type to base column column on
     * @param <B> the builder type
     * @return a new {@link ColumnDefinition.Builder}
     */
    public <B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> auditUpdateTimeColumn() {
      if (!type().isTemporal()) {
        throw new IllegalArgumentException(COLUMN_CAPTION + " " + column + " is not a temporal column");
      }

      return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>(column, UPDATE);
    }

    /**
     * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who inserted a row.
     * @param column the column
     * @param <B> the builder type
     * @return a new {@link ColumnDefinition.Builder}
     * @throws IllegalArgumentException in case column is not a string attribute
     */
    public <B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> auditInsertUserColumn() {
      if (!type().isString()) {
        throw new IllegalArgumentException(COLUMN_CAPTION + " " + column + " is not a string column");
      }

      return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>((Column<String>) column, INSERT);
    }

    /**
     * Creates a new {@link ColumnDefinition.Builder} instance, representing the username of the user who updated a row.
     * @param column the column
     * @param <B> the builder type
     * @return a new {@link ColumnDefinition.Builder}
     * @throws IllegalArgumentException in case column is not a string attribute
     */
    public <B extends ColumnDefinition.Builder<String, B>> ColumnDefinition.Builder<String, B> auditUpdateUserColumn() {
      if (!type().isString()) {
        throw new IllegalArgumentException(COLUMN_CAPTION + " " + column + " is not a string column");
      }

      return new DefaultAuditColumnDefinition.DefaultAuditColumnDefinitionBuilder<>((Column<String>) column, UPDATE);
    }
  }
}
