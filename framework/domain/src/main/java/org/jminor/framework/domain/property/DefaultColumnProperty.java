/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.ValueConverter;
import org.jminor.common.db.ValueFetcher;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
  private boolean updatable = true;
  private ForeignKeyProperty foreignKeyProperty = null;

  private final transient ValueFetcher<Object> valueFetcher;
  private final transient ResultPacker<Object> resultPacker;
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
  public final ForeignKeyProperty getForeignKeyProperty() {
    return foreignKeyProperty;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isForeignKeyProperty() {
    return foreignKeyProperty != null;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isPrimaryKeyProperty() {
    return primaryKeyIndex >= 0;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isReadOnly() {
    if (foreignKeyProperty != null) {
      return foreignKeyProperty.isReadOnly();
    }

    return super.isReadOnly();
  }

  /** {@inheritDoc} */
  @Override
  public final String getCaption() {
    final String superCaption = super.getCaption();
    if (superCaption == null && isForeignKeyProperty()) {
      return foreignKeyProperty.getCaption();
    }

    return superCaption;
  }

  /** {@inheritDoc} */
  @Override
  public final Object fetchValue(final ResultSet resultSet, final int index) throws SQLException {
    return valueFetcher.fetchValue(resultSet, index);
  }

  /** {@inheritDoc} */
  @Override
  public final ResultPacker<Object> getResultPacker() {
    return resultPacker;
  }

  void setColumnName(final String columnName) {
    this.columnName = requireNonNull(columnName, "columnName");
  }

  void setColumnHasDefaultValue(final boolean columnHasDefaultValue) {
    this.columnHasDefaultValue = columnHasDefaultValue;
  }

  void setUpdatable(final boolean updatable) {
    this.updatable = updatable;
  }

  void  setPrimaryKeyIndex(final int index) {
    if (index < 0) {
      throw new IllegalArgumentException("Primary key index must be at least 0");
    }
    this.primaryKeyIndex = index;
  }

  void setGroupingColumn(final boolean groupingColumn) {
    if (aggregateColumn) {
      throw new IllegalStateException(columnName + " is an aggregate column");
    }
    this.groupingColumn = groupingColumn;
  }

  void setAggregateColumn(final boolean aggregateColumn) {
    if (groupingColumn) {
      throw new IllegalStateException(columnName + " is a grouping column");
    }
    this.aggregateColumn = aggregateColumn;
  }

  void setSelectable(final boolean selectable) {
    this.selectable = selectable;
  }

  void setForeignKeyProperty(final ForeignKeyProperty foreignKeyProperty) {
    this.foreignKeyProperty = foreignKeyProperty;
  }

  void setReadOnly(final boolean readOnly) {
    if (isForeignKeyProperty()) {
      throw new IllegalStateException("Can not set the read only status of a property which is part of a foreign key property");
    }

    super.setReadOnly(readOnly);
  }

  void setValueConverter(final ValueConverter<?, ?> valueConverter) {
    requireNonNull(valueConverter, "valueConverter");
    this.valueConverter = (ValueConverter<Object, Object>) valueConverter;
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

  private static ValueFetcher<Object> initializeValueFetcher(final org.jminor.framework.domain.property.DefaultColumnProperty property) {
    if (property instanceof MirrorProperty) {
      return null;
    }
    switch (property.columnType) {
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
        return (resultSet, index) -> null;
      case Types.JAVA_OBJECT:
        return ResultSet::getObject;
      default:
        throw new IllegalArgumentException("Unsupported SQL value type: " + property.columnType);
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
    else {
      return null;
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
}
