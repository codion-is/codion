/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.db.result.ResultPacker;
import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.framework.domain.entity.Attribute;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.Format;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

class DefaultColumnProperty<T> extends DefaultProperty<T> implements ColumnProperty<T> {

  private static final long serialVersionUID = 1;

  private static final ValueConverter<Object, Object> DEFAULT_VALUE_CONVERTER = new DefaultValueConverter();

  private int columnType;
  private int primaryKeyIndex = -1;
  private boolean columnHasDefaultValue = false;
  private boolean insertable = true;
  private boolean updatable = true;
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
    this.columnType = getSqlType(attribute.getTypeClass());
    this.columnName = attribute.getName();
    this.valueConverter = (ValueConverter<T, Object>) DEFAULT_VALUE_CONVERTER;
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
  public final <C> C toColumnValue(final T value, final Statement statement) throws SQLException {
    return (C) valueConverter.toColumnValue(value, statement);
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
  public final T fetchValue(final ResultSet resultSet, final int index) throws SQLException {
    return valueConverter.fromColumnValue(valueFetcher.fetchValue(resultSet, index));
  }

  @Override
  public final <T> ResultPacker<T> getResultPacker() {
    return (ResultPacker<T>) resultPacker;
  }

  protected final void readOnly() {
    insertable = false;
    updatable = false;
  }

  /**
   * @return a builder for this property instance
   */
  ColumnProperty.Builder<T> builder() {
    return new DefaultColumnPropertyBuilder<>(this);
  }

  private ValueFetcher<T> initializeValueFetcher() {
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
        return this::getObject;
      default:
        throw new IllegalArgumentException("Unsupported SQL value type: " + columnType +
                ", attribute type class: " + getAttribute().getTypeClass().getName());
    }
  }

  /**
   * Returns the default sql type for the given class.
   * @param clazz the class
   * @return the corresponding sql type
   */
  private static int getSqlType(final Class<?> clazz) {
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

  private class PropertyResultPacker implements ResultPacker<T> {

    @Override
    public T fetch(final ResultSet resultSet) throws SQLException {
      return valueConverter.fromColumnValue(valueFetcher.fetchValue(resultSet, 1));
    }
  }

  private static <T> T getBoolean(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final boolean value = resultSet.getBoolean(columnIndex);

    return (T) (!value && resultSet.wasNull() ? null : value);
  }

  private static <T> T getInteger(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final int value = resultSet.getInt(columnIndex);

    return (T) (value == 0 && resultSet.wasNull() ? null : value);
  }

  private static <T> T getLong(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final long value = resultSet.getLong(columnIndex);

    return (T) (value == 0 && resultSet.wasNull() ? null : value);
  }

  private static <T> T getDouble(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final double value = resultSet.getDouble(columnIndex);

    return (T) (Double.compare(value, 0) == 0 && resultSet.wasNull() ? null : value);
  }

  private static <T> T getBigDecimal(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return (T) resultSet.getBigDecimal(columnIndex);
  }

  private static <T> T getString(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return (T) resultSet.getString(columnIndex);
  }

  private static <T> T getDate(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return (T) resultSet.getObject(columnIndex, LocalDate.class);
  }

  private static <T> T getTimestamp(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return (T) resultSet.getObject(columnIndex, LocalDateTime.class);
  }

  private static <T> T getTimestampWithTimezone(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return (T) resultSet.getObject(columnIndex, OffsetDateTime.class);
  }

  private static <T> T getTime(final ResultSet resultSet, final int columnIndex) throws SQLException {
    return (T) resultSet.getObject(columnIndex, LocalTime.class);
  }

  private static <T> T getCharacter(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final String val = getString(resultSet, columnIndex);
    if (!nullOrEmpty(val)) {
      return (T) Character.valueOf(val.charAt(0));
    }

    return null;
  }

  private static <T> T getBlob(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final Blob blob = resultSet.getBlob(columnIndex);
    if (blob == null) {
      return null;
    }

    return (T) blob.getBytes(1, (int) blob.length());
  }

  private <T> T getObject(final ResultSet resultSet, final int columnIndex) throws SQLException {
    final T object = (T) resultSet.getObject(columnIndex, getAttribute().getTypeClass());
    if (object == null) {
      return null;
    }

    return object;
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
    public T toColumnValue(final Boolean value, final Statement statement) {
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
    public Object toColumnValue(final Object value, final Statement statement) {
      return value;
    }

    @Override
    public Object fromColumnValue(final Object columnValue) {
      return columnValue;
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
    public ColumnProperty.Builder<T> captionResourceKey(final String captionResourceKey) {
      super.captionResourceKey(captionResourceKey);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> beanProperty(final String beanProperty) {
      super.beanProperty(beanProperty);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> hidden() {
      super.hidden();
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> hidden(final boolean hidden) {
      super.hidden(hidden);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> defaultValue(final T defaultValue) {
      return defaultValueSupplier(new DefaultValueSupplier<>(defaultValue));
    }

    @Override
    public final ColumnProperty.Builder<T> defaultValueSupplier(final ValueSupplier<T> supplier) {
      super.defaultValueSupplier(supplier);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> nullable(final boolean nullable) {
      super.nullable(nullable);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> maximumLength(final int maxLength) {
      super.maximumLength(maxLength);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> maximumValue(final double maximumValue) {
      super.maximumValue(maximumValue);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> minimumValue(final double minimumValue) {
      super.minimumValue(minimumValue);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> numberFormatGrouping(final boolean numberFormatGrouping) {
      super.numberFormatGrouping(numberFormatGrouping);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> preferredColumnWidth(final int preferredColumnWidth) {
      super.preferredColumnWidth(preferredColumnWidth);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> description(final String description) {
      super.description(description);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> mnemonic(final Character mnemonic) {
      super.mnemonic(mnemonic);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> format(final Format format) {
      super.format(format);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> dateTimePattern(final String dateTimePattern) {
      super.dateTimePattern(dateTimePattern);
      return this;
    }

    @Override
    public ColumnProperty.Builder<T> localeDateTimePattern(final LocaleDateTimePattern localeDateTimePattern) {
      super.localeDateTimePattern(localeDateTimePattern);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> maximumFractionDigits(final int maximumFractionDigits) {
      super.maximumFractionDigits(maximumFractionDigits);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> decimalRoundingMode(final RoundingMode decimalRoundingMode) {
      super.decimalRoundingMode(decimalRoundingMode);
      return this;
    }

    @Override
    public <C> ColumnProperty.Builder<T> columnClass(final Class<C> columnClass, final ValueConverter<T, C> valueConverter) {
      columnProperty.columnType = getSqlType(columnClass);
      columnProperty.valueConverter = (ValueConverter<T, Object>) requireNonNull(valueConverter, "valueConverter");
      columnProperty.valueFetcher = columnProperty.initializeValueFetcher();
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> columnName(final String columnName) {
      columnProperty.columnName = requireNonNull(columnName, "columnName");
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> columnHasDefaultValue() {
      columnProperty.columnHasDefaultValue = true;
      return this;
    }

    @Override
    public ColumnProperty.Builder<T> readOnly() {
      return readOnly(true);
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
        throw new IllegalArgumentException("Primary key index must be at least 0: " + property.getAttribute());
      }
      columnProperty.primaryKeyIndex = index;
      nullable(false);
      updatable(false);
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> groupingColumn() {
      if (columnProperty.aggregateColumn) {
        throw new IllegalStateException(columnProperty.columnName + " is an aggregate column: " + property.getAttribute());
      }
      columnProperty.groupingColumn = true;
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> aggregateColumn() {
      if (columnProperty.groupingColumn) {
        throw new IllegalStateException(columnProperty.columnName + " is a grouping column: " + property.getAttribute());
      }
      columnProperty.aggregateColumn = true;
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> nonSelectable() {
      columnProperty.selectable = false;
      return this;
    }

    @Override
    public final ColumnProperty.Builder<T> searchProperty() {
      if (!columnProperty.getAttribute().isString()) {
        throw new IllegalStateException("Search properties must be String based: " + property.getAttribute());
      }
      columnProperty.searchProperty = true;
      return this;
    }
  }
}
