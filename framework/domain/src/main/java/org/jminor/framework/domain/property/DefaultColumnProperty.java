/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.db.ResultPacker;

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

import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;

class DefaultColumnProperty extends DefaultProperty implements ColumnProperty {

  private static final long serialVersionUID = 1;

  private static final ValueConverter<Object, Object> DEFAULT_VALUE_CONVERTER = new DefaultValueConverter();
  private static final ValueConverter<LocalDate, java.sql.Date> DATE_VALUE_CONVERTER = new DateValueConverter();
  private static final ValueConverter<LocalDateTime, java.sql.Timestamp> TIMESTAMP_VALUE_CONVERTER = new TimestampValueConverter();
  private static final ValueConverter<LocalTime, java.sql.Time> TIME_VALUE_CONVERTER = new TimeValueConverter();

  private final int columnType;
  private int primaryKeyIndex = -1;
  private boolean columnHasDefaultValue = false;
  private boolean insertable = true;
  private boolean updatable = true;
  private boolean foreignKeyProperty = false;

  private final transient ValueFetcher valueFetcher;
  private final transient ResultPacker resultPacker;
  private transient String columnName;
  private transient ValueConverter<Object, Object> valueConverter;
  private transient boolean groupingColumn = false;
  private transient boolean aggregateColumn = false;
  private transient boolean selectable = true;

  DefaultColumnProperty(final String propertyId, final int type, final String caption) {
    this(propertyId, type, caption, type);
  }

  DefaultColumnProperty(final String propertyId, final int type, final String caption, final int columnType) {
    super(propertyId, type, caption, getTypeClass(type));
    this.columnName = propertyId;
    this.columnType = columnType;
    this.valueConverter = initializeValueConverter(this);
    this.valueFetcher = initializeValueFetcher(this);
    this.resultPacker = new PropertyResultPacker();
  }

  /** {@inheritDoc} */
  @Override
  public final String getColumnName() {
    return columnName;
  }

  /** {@inheritDoc} */
  @Override
  public final int getColumnType() {
    return columnType;
  }

  /** {@inheritDoc} */
  @Override
  public final Object toColumnValue(final Object value) {
    return valueConverter.toColumnValue(value);
  }

  /** {@inheritDoc} */
  @Override
  public final Object fromColumnValue(final Object object) {
    return valueConverter.fromColumnValue(object);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean columnHasDefaultValue() {
    return columnHasDefaultValue;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isInsertable() {
    return insertable;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isUpdatable() {
    return this.updatable;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isDenormalized() {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final int getPrimaryKeyIndex() {
    return primaryKeyIndex;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isGroupingColumn() {
    return groupingColumn;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isAggregateColumn() {
    return aggregateColumn;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isSelectable() {
    return selectable;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isForeignKeyProperty() {
    return foreignKeyProperty;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isPrimaryKeyProperty() {
    return primaryKeyIndex >= 0;
  }

  /** {@inheritDoc} */
  @Override
  public final <T> T fetchValue(final ResultSet resultSet, final int index) throws SQLException {
    return (T) valueFetcher.fetchValue(resultSet, index);
  }

  /** {@inheritDoc} */
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
  ColumnProperty.Builder builder() {
    return new DefaultColumnPropertyBuilder(this);
  }

  private static ValueConverter initializeValueConverter(final ColumnProperty property) {
    if (property.isDate()) {
      return DATE_VALUE_CONVERTER;
    }
    else if (property.isTimestamp()) {
      return TIMESTAMP_VALUE_CONVERTER;
    }
    else if (property.isTime()) {
      return TIME_VALUE_CONVERTER;
    }

    return DEFAULT_VALUE_CONVERTER;
  }

  private static ValueFetcher initializeValueFetcher(final ColumnProperty property) {
    if (property instanceof MirrorProperty) {
      return null;
    }
    switch (property.getColumnType()) {
      case Types.INTEGER:
        return (resultSet, index) -> property.fromColumnValue(getInteger(resultSet, index));
      case Types.BIGINT:
        return (resultSet, index) -> property.fromColumnValue(getLong(resultSet, index));
      case Types.DOUBLE:
        return (resultSet, index) -> property.fromColumnValue(getDouble(resultSet, index));
      case Types.DECIMAL:
        return (resultSet, index) -> property.fromColumnValue(getBigDecimal(resultSet, index));
      case Types.DATE:
        return (resultSet, index) -> property.fromColumnValue(getDate(resultSet, index));
      case Types.TIMESTAMP:
        return (resultSet, index) -> property.fromColumnValue(getTimestamp(resultSet, index));
      case Types.TIME:
        return (resultSet, index) -> property.fromColumnValue(getTime(resultSet, index));
      case Types.VARCHAR:
        return (resultSet, index) -> property.fromColumnValue(getString(resultSet, index));
      case Types.BOOLEAN:
        return (resultSet, index) -> property.fromColumnValue(getBoolean(resultSet, index));
      case Types.CHAR:
        return (resultSet, index) -> property.fromColumnValue(getCharacter(resultSet, index));
      case Types.BLOB:
        return (resultSet, index) -> property.fromColumnValue(getBlob(resultSet, index));
      case Types.JAVA_OBJECT:
        return ResultSet::getObject;
      default:
        throw new IllegalArgumentException("Unsupported SQL value type: " + property.getColumnType());
    }
  }

  private final class PropertyResultPacker implements ResultPacker<Object> {

    private static final int COLUMN_INDEX = 1;

    @Override
    public Object fetch(final ResultSet resultSet) throws SQLException {
      if (isInteger()) {
        return getInteger(resultSet, COLUMN_INDEX);
      }
      else if (isLong()) {
        return getLong(resultSet, COLUMN_INDEX);
      }
      else if (isDouble()) {
        return getDouble(resultSet, COLUMN_INDEX);
      }

      return resultSet.getObject(COLUMN_INDEX);
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

  static class DefaultColumnPropertyBuilder extends DefaultPropertyBuilder implements ColumnProperty.Builder {

    private final DefaultColumnProperty columnProperty;

    DefaultColumnPropertyBuilder(final DefaultColumnProperty columnProperty) {
      super(columnProperty);
      this.columnProperty = columnProperty;
    }

    @Override
    public ColumnProperty get() {
      return columnProperty;
    }

    @Override
    public final ColumnProperty.Builder columnName(final String columnName) {
      columnProperty.columnName = requireNonNull(columnName, "columnName");
      return this;
    }

    @Override
    public final ColumnProperty.Builder columnHasDefaultValue(final boolean columnHasDefaultValue) {
      columnProperty.columnHasDefaultValue = columnHasDefaultValue;
      return this;
    }

    @Override
    public ColumnProperty.Builder readOnly(final boolean readOnly) {
      columnProperty.insertable = !readOnly;
      columnProperty.updatable = !readOnly;
      return this;
    }

    @Override
    public ColumnProperty.Builder insertable(final boolean insertable) {
      columnProperty.insertable = insertable;
      return this;
    }

    @Override
    public ColumnProperty.Builder updatable(final boolean updatable) {
      columnProperty.updatable = updatable;
      return this;
    }

    @Override
    public final ColumnProperty.Builder primaryKeyIndex(final int index) {
      if (index < 0) {
        throw new IllegalArgumentException("Primary key index must be at least 0");
      }
      columnProperty.primaryKeyIndex = index;
      nullable(false);
      updatable(false);
      return this;
    }

    @Override
    public final ColumnProperty.Builder groupingColumn(final boolean groupingColumn) {
      if (columnProperty.aggregateColumn) {
        throw new IllegalStateException(columnProperty.columnName + " is an aggregate column");
      }
      columnProperty.groupingColumn = groupingColumn;
      return this;
    }

    @Override
    public final ColumnProperty.Builder aggregateColumn(final boolean aggregateColumn) {
      if (columnProperty.groupingColumn) {
        throw new IllegalStateException(columnProperty.columnName + " is a grouping column");
      }
      columnProperty.aggregateColumn = aggregateColumn;
      return this;
    }

    @Override
    public final ColumnProperty.Builder selectable(final boolean selectable) {
      columnProperty.selectable = selectable;
      return this;
    }

    @Override
    public final ColumnProperty.Builder valueConverter(final ValueConverter<?, ?> valueConverter) {
      requireNonNull(valueConverter, "valueConverter");
      columnProperty.valueConverter = (ValueConverter<Object, Object>) valueConverter;
      return this;
    }

    @Override
    public final void setForeignKeyProperty(final boolean foreignKeyProperty) {
      columnProperty.foreignKeyProperty = foreignKeyProperty;
    }
  }
}
