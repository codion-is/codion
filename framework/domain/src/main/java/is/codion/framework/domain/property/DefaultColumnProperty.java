/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.db.result.ResultPacker;
import is.codion.framework.domain.entity.Attribute;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

class DefaultColumnProperty<T> extends AbstractProperty<T> implements ColumnProperty<T> {

  private static final long serialVersionUID = 1;

  private static final ValueConverter<Object, Object> DEFAULT_VALUE_CONVERTER = new DefaultValueConverter();

  private final int columnType;
  private final int primaryKeyIndex;
  private final boolean columnHasDefaultValue;
  private final boolean insertable;
  private final boolean updatable;
  private final boolean searchProperty;

  private final transient ResultPacker<T> resultPacker = new PropertyResultPacker();
  private final transient String columnName;
  private final transient String columnExpression;
  private final transient ValueFetcher<T> valueFetcher;
  private final transient ValueConverter<T, Object> valueConverter;
  private final transient boolean groupingColumn;
  private final transient boolean aggregateColumn;
  private final transient boolean selectable;

  protected DefaultColumnProperty(DefaultColumnPropertyBuilder<T, ?> builder) {
    super(builder);
    this.columnType = builder.columnType;
    this.primaryKeyIndex = builder.primaryKeyIndex;
    this.columnHasDefaultValue = builder.columnHasDefaultValue;
    this.insertable = builder.insertable;
    this.updatable = builder.updatable;
    this.searchProperty = builder.searchProperty;
    this.columnName = builder.columnName;
    this.columnExpression = builder.columnExpression;
    this.valueFetcher = builder.valueFetcher;
    this.valueConverter = builder.valueConverter;
    this.groupingColumn = builder.groupingColumn;
    this.aggregateColumn = builder.aggregateColumn;
    this.selectable = builder.selectable;
  }

  @Override
  public final String getColumnName() {
    return columnName;
  }

  @Override
  public final String getColumnExpression() {
    return columnExpression == null ? columnName : columnExpression;
  }

  @Override
  public final int getColumnType() {
    return columnType;
  }

  @Override
  public final <C> C toColumnValue(T value, Statement statement) throws SQLException {
    return (C) valueConverter.toColumnValue(value, statement);
  }

  @Override
  public final boolean columnHasDefaultValue() {
    return columnHasDefaultValue;
  }

  @Override
  public final boolean isInsertable() {
    return insertable;
  }

  @Override
  public final boolean isUpdatable() {
    return updatable;
  }

  @Override
  public final boolean isReadOnly() {
    return !insertable && !updatable;
  }

  @Override
  public boolean isDenormalized() {
    return false;
  }

  @Override
  public final int getPrimaryKeyIndex() {
    return primaryKeyIndex;
  }

  @Override
  public final boolean isGroupingColumn() {
    return groupingColumn;
  }

  @Override
  public final boolean isAggregateColumn() {
    return aggregateColumn;
  }

  @Override
  public final boolean isSelectable() {
    return selectable;
  }

  @Override
  public final boolean isPrimaryKeyColumn() {
    return primaryKeyIndex >= 0;
  }

  @Override
  public final boolean isSearchProperty() {
    return searchProperty;
  }

  @Override
  public final T fetchValue(ResultSet resultSet, int index) throws SQLException {
    return valueConverter.fromColumnValue(valueFetcher.fetchValue(resultSet, index));
  }

  @Override
  public final ResultPacker<T> getResultPacker() {
    return resultPacker;
  }

  private class PropertyResultPacker implements ResultPacker<T> {

    @Override
    public T fetch(ResultSet resultSet) throws SQLException {
      return valueConverter.fromColumnValue(valueFetcher.fetchValue(resultSet, 1));
    }
  }

  private static <T> T getBoolean(ResultSet resultSet, int columnIndex) throws SQLException {
    boolean value = resultSet.getBoolean(columnIndex);

    return (T) (!value && resultSet.wasNull() ? null : value);
  }

  private static <T> T getInteger(ResultSet resultSet, int columnIndex) throws SQLException {
    int value = resultSet.getInt(columnIndex);

    return (T) (value == 0 && resultSet.wasNull() ? null : value);
  }

  private static <T> T getLong(ResultSet resultSet, int columnIndex) throws SQLException {
    long value = resultSet.getLong(columnIndex);

    return (T) (value == 0L && resultSet.wasNull() ? null : value);
  }

  private static <T> T getDouble(ResultSet resultSet, int columnIndex) throws SQLException {
    double value = resultSet.getDouble(columnIndex);

    return (T) (Double.compare(value, 0d) == 0 && resultSet.wasNull() ? null : value);
  }

  private static <T> T getBigDecimal(ResultSet resultSet, int columnIndex) throws SQLException {
    return (T) resultSet.getBigDecimal(columnIndex);
  }

  private static <T> T getString(ResultSet resultSet, int columnIndex) throws SQLException {
    return (T) resultSet.getString(columnIndex);
  }

  private static <T> T getDate(ResultSet resultSet, int columnIndex) throws SQLException {
    return (T) resultSet.getObject(columnIndex, LocalDate.class);
  }

  private static <T> T getTimestamp(ResultSet resultSet, int columnIndex) throws SQLException {
    return (T) resultSet.getObject(columnIndex, LocalDateTime.class);
  }

  private static <T> T getTimestampWithTimezone(ResultSet resultSet, int columnIndex) throws SQLException {
    return (T) resultSet.getObject(columnIndex, OffsetDateTime.class);
  }

  private static <T> T getTime(ResultSet resultSet, int columnIndex) throws SQLException {
    return (T) resultSet.getObject(columnIndex, LocalTime.class);
  }

  private static <T> T getCharacter(ResultSet resultSet, int columnIndex) throws SQLException {
    String string = getString(resultSet, columnIndex);
    if (nullOrEmpty(string)) {
      return null;
    }

    return (T) Character.valueOf(string.charAt(0));
  }

  private static <T> T getBlob(ResultSet resultSet, int columnIndex) throws SQLException {
    return (T) resultSet.getBytes(columnIndex);
  }

  private static <T> T getObject(ResultSet resultSet, int columnIndex, Class<T> typeClass) throws SQLException {
    return resultSet.getObject(columnIndex, typeClass);
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

  private static final class DefaultValueConverter implements ValueConverter<Object, Object> {
    @Override
    public Object toColumnValue(Object value, Statement statement) {
      return value;
    }

    @Override
    public Object fromColumnValue(Object columnValue) {
      return columnValue;
    }
  }

  static class DefaultColumnPropertyBuilder<T, B extends ColumnProperty.Builder<T, B>>
          extends AbstractPropertyBuilder<T, B> implements ColumnProperty.Builder<T, B> {

    private int columnType;
    private int primaryKeyIndex;
    private boolean columnHasDefaultValue;
    private boolean insertable;
    private boolean updatable;
    private boolean searchProperty;
    private String columnName;
    private String columnExpression;
    private ValueFetcher<T> valueFetcher;
    private ValueConverter<T, Object> valueConverter;
    private boolean groupingColumn;
    private boolean aggregateColumn;
    private boolean selectable;

    DefaultColumnPropertyBuilder(Attribute<T> attribute, String caption) {
      super(attribute, caption);
      this.primaryKeyIndex = -1;
      this.columnType = getSqlType(attribute.getTypeClass());
      this.columnHasDefaultValue = false;
      this.insertable = true;
      this.updatable = true;
      this.searchProperty = false;
      this.columnName = attribute.getName();
      this.valueFetcher = initializeValueFetcher(this.columnType, attribute.getTypeClass());
      this.valueConverter = (ValueConverter<T, Object>) DEFAULT_VALUE_CONVERTER;
      this.groupingColumn = false;
      this.aggregateColumn = false;
      this.selectable = true;
    }

    @Override
    public Property<T> build() {
      return new DefaultColumnProperty<>(this);
    }

    @Override
    public final <C> B columnClass(Class<C> columnClass, ValueConverter<T, C> valueConverter) {
      this.columnType = getSqlType(columnClass);
      this.valueConverter = (ValueConverter<T, Object>) requireNonNull(valueConverter, "valueConverter");
      this.valueFetcher = initializeValueFetcher(this.columnType, attribute.getTypeClass());
      return (B) this;
    }

    @Override
    public final <C> B columnClass(Class<C> columnClass, ValueConverter<T, C> valueConverter,
                                   ValueFetcher<C> valueFetcher) {
      this.columnType = getSqlType(columnClass);
      this.valueConverter = (ValueConverter<T, Object>) requireNonNull(valueConverter, "valueConverter");
      this.valueFetcher = (ValueFetcher<T>) requireNonNull(valueFetcher, "valueFetcher");
      return (B) this;
    }

    @Override
    public final B columnName(String columnName) {
      this.columnName = requireNonNull(columnName, "columnName");
      return (B) this;
    }

    @Override
    public final B columnExpression(String columnExpression) {
      this.columnExpression = requireNonNull(columnExpression, "columnExpression");
      return (B) this;
    }

    @Override
    public final B columnHasDefaultValue() {
      this.columnHasDefaultValue = true;
      return (B) this;
    }

    @Override
    public B readOnly() {
      return readOnly(true);
    }

    @Override
    public B readOnly(boolean readOnly) {
      this.insertable = !readOnly;
      this.updatable = !readOnly;
      return (B) this;
    }

    @Override
    public B insertable(boolean insertable) {
      this.insertable = insertable;
      return (B) this;
    }

    @Override
    public B updatable(boolean updatable) {
      this.updatable = updatable;
      return (B) this;
    }

    @Override
    public final B primaryKeyIndex(int index) {
      if (index < 0) {
        throw new IllegalArgumentException("Primary key index must be at least 0: " + attribute);
      }
      this.primaryKeyIndex = index;
      nullable(false);
      updatable(false);
      return (B) this;
    }

    @Override
    public final B groupingColumn() {
      if (this.aggregateColumn) {
        throw new IllegalStateException(this.columnName + " is an aggregate column: " + attribute);
      }
      this.groupingColumn = true;
      return (B) this;
    }

    @Override
    public final B aggregateColumn() {
      if (this.groupingColumn) {
        throw new IllegalStateException(this.columnName + " is a grouping column: " + attribute);
      }
      this.aggregateColumn = true;
      return (B) this;
    }

    @Override
    public final B nonSelectable() {
      this.selectable = false;
      return (B) this;
    }

    @Override
    public final B searchProperty() {
      if (!attribute.isString()) {
        throw new IllegalStateException("Search properties must be String based: " + attribute);
      }
      this.searchProperty = true;
      return (B) this;
    }

    /**
     * Returns the default sql type for the given class.
     * @param clazz the class
     * @return the corresponding sql type
     */
    private static int getSqlType(Class<?> clazz) {
      requireNonNull(clazz, "clazz");
      if (clazz.equals(Long.class)) {
        return Types.BIGINT;
      }
      if (clazz.equals(Integer.class)) {
        return Types.INTEGER;
      }
      if (clazz.equals(Double.class)) {
        return Types.DOUBLE;
      }
      if (clazz.equals(BigDecimal.class)) {
        return Types.DECIMAL;
      }
      if (clazz.equals(LocalDate.class)) {
        return Types.DATE;
      }
      if (clazz.equals(LocalTime.class)) {
        return Types.TIME;
      }
      if (clazz.equals(LocalDateTime.class)) {
        return Types.TIMESTAMP;
      }
      if (clazz.equals(OffsetDateTime.class)) {
        return Types.TIMESTAMP_WITH_TIMEZONE;
      }
      if (clazz.equals(String.class)) {
        return Types.VARCHAR;
      }
      if (clazz.equals(Boolean.class)) {
        return Types.BOOLEAN;
      }
      if (clazz.equals(byte[].class)) {
        return Types.BLOB;
      }

      return Types.OTHER;
    }

    private static <T> ValueFetcher<T> initializeValueFetcher(int columnType, Class<T> typeClass) {
      switch (columnType) {
        case Types.INTEGER:
          return DefaultColumnProperty::getInteger;
        case Types.BIGINT:
          return DefaultColumnProperty::getLong;
        case Types.DOUBLE:
          return DefaultColumnProperty::getDouble;
        case Types.DECIMAL:
          return DefaultColumnProperty::getBigDecimal;
        case Types.DATE:
          return DefaultColumnProperty::getDate;
        case Types.TIMESTAMP:
          return DefaultColumnProperty::getTimestamp;
        case Types.TIMESTAMP_WITH_TIMEZONE:
          return DefaultColumnProperty::getTimestampWithTimezone;
        case Types.TIME:
          return DefaultColumnProperty::getTime;
        case Types.VARCHAR:
          return DefaultColumnProperty::getString;
        case Types.BOOLEAN:
          return DefaultColumnProperty::getBoolean;
        case Types.CHAR:
          return DefaultColumnProperty::getCharacter;
        case Types.BLOB:
          return DefaultColumnProperty::getBlob;
        case Types.OTHER:
          return (resultSet, index) -> DefaultColumnProperty.getObject(resultSet, index, typeClass);
        default:
          throw new IllegalArgumentException("Unsupported SQL value type: " + columnType +
                  ", attribute type class: " + typeClass.getName());
      }
    }
  }
  
  static abstract class AbstractReadOnlyColumnPropertyBuilder<T, B extends ColumnProperty.Builder<T, B>>
          extends DefaultColumnPropertyBuilder<T, B> implements Property.Builder<T, B> {

    protected AbstractReadOnlyColumnPropertyBuilder(Attribute<T> attribute, String caption) {
      super(attribute, caption);
      super.readOnly(true);
    }

    @Override
    public final B readOnly() {
      throw new UnsupportedOperationException("Read only by default: " + attribute);
    }

    @Override
    public final B readOnly(boolean readOnly) {
      throw new UnsupportedOperationException("Read only by default: " + attribute);
    }

    @Override
    public final B insertable(boolean insertable) {
      throw new UnsupportedOperationException("Property is not insertable: " + attribute);
    }

    @Override
    public final B updatable(boolean updatable) {
      throw new UnsupportedOperationException("Property is not updatable: " + attribute);
    }
  }
}
