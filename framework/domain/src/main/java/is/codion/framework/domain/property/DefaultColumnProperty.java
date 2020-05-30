/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.db.result.ResultPacker;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

class DefaultColumnProperty<T> extends DefaultProperty<T> implements ColumnProperty<T> {

  private static final long serialVersionUID = 1;

  private static final ValueConverter<Object, Object> DEFAULT_VALUE_CONVERTER = new DefaultValueConverter();
  private static final ValueConverter<LocalDate, java.sql.Date> DATE_VALUE_CONVERTER = new DateValueConverter();
  private static final ValueConverter<LocalDateTime, java.sql.Timestamp> TIMESTAMP_VALUE_CONVERTER = new TimestampValueConverter();
  private static final ValueConverter<LocalTime, java.sql.Time> TIME_VALUE_CONVERTER = new TimeValueConverter();

  private int columnType;
  private int primaryKeyIndex = -1;
  private boolean columnHasDefaultValue = false;
  private boolean insertable = true;
  private boolean updatable = true;
  private boolean foreignKeyProperty = false;
  private boolean searchProperty = false;

  private final transient ResultPacker<T> resultPacker;
  private transient ValueFetcher<T> valueFetcher;
  private transient String columnName;
  private transient ValueConverter<T, Object> valueConverter;
  private transient boolean groupingColumn = false;
  private transient boolean aggregateColumn = false;
  private transient boolean selectable = true;

  DefaultColumnProperty(final Attribute<T> attribute, final String caption) {
    super(attribute, caption);
    this.columnType = attribute.getType();
    this.columnName = attribute.getName();
    this.valueConverter = initializeValueConverter();
    this.valueFetcher = initializeValueFetcher();
    this.resultPacker = new PropertyResultPacker();
  }

  @Override
  public final String getColumnName() {
    return columnName;
  }

  @Override
  public final int getColumnType() {
    return columnType;
  }

  @Override
  public final Object toColumnValue(final T value) {
    return valueConverter.toColumnValue(value);
  }

  @Override
  public final T fromColumnValue(final Object object) {
    return valueConverter.fromColumnValue(object);
  }

  @Override
  public final boolean columnHasDefaultValue() {
    return columnHasDefaultValue;
  }

  @Override
  public boolean isInsertable() {
    return insertable;
  }

  @Override
  public final boolean isUpdatable() {
    return this.updatable;
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
  public final boolean isForeignKeyProperty() {
    return foreignKeyProperty;
  }

  @Override
  public final boolean isPrimaryKeyProperty() {
    return primaryKeyIndex >= 0;
  }

  @Override
  public final boolean isSearchProperty() {
    return searchProperty;
  }

  @Override
  public final <T> T fetchValue(final ResultSet resultSet, final int index) throws SQLException {
    return (T) valueFetcher.fetchValue(resultSet, index);
  }

  @Override
  public final <T> ResultPacker<T> getResultPacker() {
    return (ResultPacker<T>) resultPacker;
  }

  protected final void setInsertable(final boolean insertable) {
    this.insertable = insertable;
  }

  protected final void setUpdatable(final boolean updatable) {
    this.updatable = updatable;
  }

  /**
   * @return a builder for this property instance
   */
  ColumnProperty.Builder<T> builder() {
    return new DefaultColumnPropertyBuilder<>(this);
  }

  private ValueConverter initializeValueConverter() {
    if (isDate()) {
      return DATE_VALUE_CONVERTER;
    }
    else if (isTimestamp()) {
      return TIMESTAMP_VALUE_CONVERTER;
    }
    else if (isTime()) {
      return TIME_VALUE_CONVERTER;
    }

    return DEFAULT_VALUE_CONVERTER;
  }

  private ValueFetcher initializeValueFetcher() {
    if (this instanceof MirrorProperty) {
      return null;
    }
    switch (columnType) {
      case Types.INTEGER:
        return (resultSet, columnIndex) -> fromColumnValue(getInteger(resultSet, columnIndex));
      case Types.BIGINT:
        return (resultSet, columnIndex) -> fromColumnValue(getLong(resultSet, columnIndex));
      case Types.DOUBLE:
        return (resultSet, columnIndex) -> fromColumnValue(getDouble(resultSet, columnIndex));
      case Types.DECIMAL:
        return (resultSet, columnIndex) -> fromColumnValue(getBigDecimal(resultSet, columnIndex));
      case Types.DATE:
        return (resultSet, columnIndex) -> fromColumnValue(getDate(resultSet, columnIndex));
      case Types.TIMESTAMP:
        return (resultSet, columnIndex) -> fromColumnValue(getTimestamp(resultSet, columnIndex));
      case Types.TIME:
        return (resultSet, columnIndex) -> fromColumnValue(getTime(resultSet, columnIndex));
      case Types.VARCHAR:
        return (resultSet, columnIndex) -> fromColumnValue(getString(resultSet, columnIndex));
      case Types.BOOLEAN:
        return (resultSet, columnIndex) -> fromColumnValue(getBoolean(resultSet, columnIndex));
      case Types.CHAR:
        return (resultSet, columnIndex) -> fromColumnValue(getCharacter(resultSet, columnIndex));
      case Types.BLOB:
        return (resultSet, columnIndex) -> fromColumnValue(getBlob(resultSet, columnIndex));
      case Types.JAVA_OBJECT:
        return ResultSet::getObject;
      default:
        throw new IllegalArgumentException("Unsupported SQL value type: " + getColumnType());
    }
  }

  private class PropertyResultPacker implements ResultPacker<T> {

    @Override
    public T fetch(final ResultSet resultSet) throws SQLException {
      return valueFetcher.fetchValue(resultSet, 1);
    }
  }

  private static Boolean getBoolean(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final boolean value = resultSet.getBoolean(columnIndex);

    return !value && resultSet.wasNull() ? null : value;
  }

  private static Integer getInteger(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final int value = resultSet.getInt(columnIndex);

    return value == 0 && resultSet.wasNull() ? null : value;
  }

  private static Long getLong(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final long value = resultSet.getLong(columnIndex);

    return value == 0 && resultSet.wasNull() ? null : value;
  }

  private static Double getDouble(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final double value = resultSet.getDouble(columnIndex);

    return Double.compare(value, 0) == 0 && resultSet.wasNull() ? null : value;
  }

  private static BigDecimal getBigDecimal(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return resultSet.getBigDecimal(columnIndex);
  }

  private static String getString(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return resultSet.getString(columnIndex);
  }

  private static java.util.Date getDate(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return resultSet.getDate(columnIndex);
  }

  private static Timestamp getTimestamp(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return resultSet.getTimestamp(columnIndex);
  }

  private static Time getTime(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return resultSet.getTime(columnIndex);
  }

  private static Character getCharacter(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final String val = getString(resultSet, columnIndex);
    if (!nullOrEmpty(val)) {
      return val.charAt(0);
    }

    return null;
  }

  private static byte[] getBlob(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final Blob blob = resultSet.getBlob(columnIndex);
    if (blob == null) {
      return null;
    }

    return blob.getBytes(1, (int) blob.length());
  }

  static final class BooleanValueConverter<T> implements ValueConverter<Boolean, T> {

    private final T trueValue;
    private final T falseValue;

    BooleanValueConverter(final T trueValue, final T falseValue) {
      this.trueValue = requireNonNull(trueValue);
      this.falseValue = requireNonNull(falseValue);
    }

    @Override
    public Boolean fromColumnValue(final T columnValue) {
      if (Objects.equals(trueValue, columnValue)) {
        return true;
      }
      else if (Objects.equals(falseValue, columnValue)) {
        return false;
      }

      return null;
    }

    @Override
    public T toColumnValue(final Boolean value) {
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
    public Object toColumnValue(final Object value) {
      return value;
    }

    @Override
    public Object fromColumnValue(final Object columnValue) {
      return columnValue;
    }
  }

  private static final class DateValueConverter implements ValueConverter<LocalDate, java.sql.Date> {
    @Override
    public java.sql.Date toColumnValue(final LocalDate value) {
      if (value == null) {
        return null;
      }

      return java.sql.Date.valueOf(value);
    }

    @Override
    public LocalDate fromColumnValue(final java.sql.Date columnValue) {
      if (columnValue == null) {
        return null;
      }

      return columnValue.toLocalDate();
    }
  }

  private static final class TimestampValueConverter implements ValueConverter<LocalDateTime, java.sql.Timestamp> {
    @Override
    public java.sql.Timestamp toColumnValue(final LocalDateTime value) {
      if (value == null) {
        return null;
      }

      return java.sql.Timestamp.valueOf(value);
    }

    @Override
    public LocalDateTime fromColumnValue(final java.sql.Timestamp columnValue) {
      if (columnValue == null) {
        return null;
      }

      return columnValue.toLocalDateTime();
    }
  }

  private static final class TimeValueConverter implements ValueConverter<LocalTime, java.sql.Time> {
    @Override
    public java.sql.Time toColumnValue(final LocalTime value) {
      if (value == null) {
        return null;
      }

      return java.sql.Time.valueOf(value);
    }

    @Override
    public LocalTime fromColumnValue(final java.sql.Time columnValue) {
      if (columnValue == null) {
        return null;
      }

      return columnValue.toLocalTime();
    }
  }

  static class DefaultColumnPropertyBuilder<T> extends DefaultPropertyBuilder<T> implements ColumnProperty.Builder<T> {

    private final DefaultColumnProperty<T> columnProperty;

    DefaultColumnPropertyBuilder(final DefaultColumnProperty<T> columnProperty) {
      super(columnProperty);
      this.columnProperty = columnProperty;
    }

    @Override
    public ColumnProperty<T> get() {
      return columnProperty;
    }

    @Override
    public final ColumnProperty.Builder<T> columnType(final int columnType) {
      columnProperty.columnType = columnType;
      columnProperty.valueFetcher = columnProperty.initializeValueFetcher();
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> columnName(final String columnName) {
      columnProperty.columnName = requireNonNull(columnName, "columnName");
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> columnHasDefaultValue(final boolean columnHasDefaultValue) {
      columnProperty.columnHasDefaultValue = columnHasDefaultValue;
      return this;
    }

    @Override
    public ColumnProperty.Builder<T> readOnly(final boolean readOnly) {
      columnProperty.insertable = !readOnly;
      columnProperty.updatable = !readOnly;
      return this;
    }

    @Override
    public ColumnProperty.Builder<T> insertable(final boolean insertable) {
      columnProperty.insertable = insertable;
      return this;
    }

    @Override
    public ColumnProperty.Builder<T> updatable(final boolean updatable) {
      columnProperty.updatable = updatable;
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> primaryKeyIndex(final int index) {
      if (index < 0) {
        throw new IllegalArgumentException("Primary key index must be at least 0");
      }
      columnProperty.primaryKeyIndex = index;
      nullable(false);
      updatable(false);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> groupingColumn(final boolean groupingColumn) {
      if (columnProperty.aggregateColumn) {
        throw new IllegalStateException(columnProperty.columnName + " is an aggregate column");
      }
      columnProperty.groupingColumn = groupingColumn;
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> aggregateColumn(final boolean aggregateColumn) {
      if (columnProperty.groupingColumn) {
        throw new IllegalStateException(columnProperty.columnName + " is a grouping column");
      }
      columnProperty.aggregateColumn = aggregateColumn;
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> selectable(final boolean selectable) {
      columnProperty.selectable = selectable;
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> valueConverter(final ValueConverter<T, Object> valueConverter) {
      requireNonNull(valueConverter, "valueConverter");
      columnProperty.valueConverter = valueConverter;
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> searchProperty(final boolean searchProperty) {
      if (searchProperty && columnProperty.columnType != Types.VARCHAR) {
        throw new IllegalStateException("Search properties must be of type Types.VARCHAR");
      }
      columnProperty.searchProperty = searchProperty;
      return this;
    }

    @Override
    public final void setForeignKeyProperty(final boolean foreignKeyProperty) {
      columnProperty.foreignKeyProperty = foreignKeyProperty;
    }
  }
}
