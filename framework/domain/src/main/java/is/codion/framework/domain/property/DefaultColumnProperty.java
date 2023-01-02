/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.db.result.ResultPacker;
import is.codion.framework.domain.entity.Attribute;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

class DefaultColumnProperty<T> extends AbstractProperty<T> implements ColumnProperty<T> {

  private static final long serialVersionUID = 1;

  private static final ValueConverter<Object, Object> DEFAULT_VALUE_CONVERTER = new DefaultValueConverter();
  private static final Map<Class<?>, Integer> TYPE_MAP = createTypeMap();
  private static final Map<Integer, ValueFetcher<?>> VALUE_FETCHERS = createValueFetchers();

  private final int columnType;
  private final int primaryKeyIndex;
  private final boolean columnHasDefaultValue;
  private final boolean insertable;
  private final boolean updatable;
  private final boolean searchProperty;

  private final transient ResultPacker<T> resultPacker = new PropertyResultPacker();
  private final transient String columnName;
  private final transient String columnExpression;
  private final transient ValueFetcher<Object> valueFetcher;
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
  public final String columnName() {
    return columnName;
  }

  @Override
  public final String columnExpression() {
    return columnExpression == null ? columnName : columnExpression;
  }

  @Override
  public final int columnType() {
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
  public final int primaryKeyIndex() {
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
  public final ResultPacker<T> resultPacker() {
    return resultPacker;
  }

  private class PropertyResultPacker implements ResultPacker<T> {

    @Override
    public T fetch(ResultSet resultSet) throws SQLException {
      return valueConverter.fromColumnValue(valueFetcher.fetchValue(resultSet, 1));
    }
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

  private static Map<Class<?>, Integer> createTypeMap() {
    Map<Class<?>, Integer> typeMap = new HashMap<>();
    typeMap.put(Long.class, Types.BIGINT);
    typeMap.put(Integer.class, Types.INTEGER);
    typeMap.put(Short.class, Types.SMALLINT);
    typeMap.put(Double.class, Types.DOUBLE);
    typeMap.put(BigDecimal.class, Types.DECIMAL);
    typeMap.put(LocalDate.class, Types.DATE);
    typeMap.put(LocalTime.class, Types.TIME);
    typeMap.put(LocalDateTime.class, Types.TIMESTAMP);
    typeMap.put(OffsetDateTime.class, Types.TIMESTAMP_WITH_TIMEZONE);
    typeMap.put(java.util.Date.class, Types.DATE);
    typeMap.put(java.sql.Date.class, Types.DATE);
    typeMap.put(Time.class, Types.TIME);
    typeMap.put(Timestamp.class, Types.TIMESTAMP);
    typeMap.put(String.class, Types.VARCHAR);
    typeMap.put(Character.class, Types.CHAR);
    typeMap.put(Boolean.class, Types.BOOLEAN);
    typeMap.put(byte[].class, Types.BLOB);

    return typeMap;
  }

  private static Map<Integer, ValueFetcher<?>> createValueFetchers() {
    Map<Integer, ValueFetcher<?>> valueFetchers = new HashMap<>();
    valueFetchers.put(Types.SMALLINT, new ShortFetcher());
    valueFetchers.put(Types.INTEGER, new IntegerFetcher());
    valueFetchers.put(Types.BIGINT, new LongFetcher());
    valueFetchers.put(Types.DOUBLE, new DoubleFetcher());
    valueFetchers.put(Types.DECIMAL, new BigDecimalFetcher());
    valueFetchers.put(Types.DATE, new LocalDateFetcher());
    valueFetchers.put(Types.TIMESTAMP, new LocalDateTimeFetcher());
    valueFetchers.put(Types.TIMESTAMP_WITH_TIMEZONE, new OffsetDateTimeFetcher());
    valueFetchers.put(Types.TIME, new LocalTimeFetcher());
    valueFetchers.put(Types.VARCHAR, new StringFetcher());
    valueFetchers.put(Types.BOOLEAN, new BooleanFetcher());
    valueFetchers.put(Types.CHAR, new CharacterFetcher());
    valueFetchers.put(Types.BLOB, new ByteArrayFetcher());

    return valueFetchers;
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
    private ValueFetcher<Object> valueFetcher;
    private ValueConverter<T, Object> valueConverter;
    private boolean groupingColumn;
    private boolean aggregateColumn;
    private boolean selectable;

    DefaultColumnPropertyBuilder(Attribute<T> attribute, String caption) {
      super(attribute, caption);
      this.primaryKeyIndex = -1;
      this.columnType = sqlType(attribute.valueClass());
      this.columnHasDefaultValue = false;
      this.insertable = true;
      this.updatable = true;
      this.searchProperty = false;
      this.columnName = attribute.name();
      this.valueFetcher = (ValueFetcher<Object>) valueFetcher(this.columnType, attribute.valueClass());
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
      this.columnType = sqlType(columnClass);
      this.valueConverter = (ValueConverter<T, Object>) requireNonNull(valueConverter, "valueConverter");
      this.valueFetcher = (ValueFetcher<Object>) valueFetcher(this.columnType, attribute.valueClass());
      return (B) this;
    }

    @Override
    public final <C> B columnClass(Class<C> columnClass, ValueConverter<T, C> valueConverter,
                                   ValueFetcher<C> valueFetcher) {
      this.columnType = sqlType(columnClass);
      this.valueConverter = (ValueConverter<T, Object>) requireNonNull(valueConverter, "valueConverter");
      this.valueFetcher = (ValueFetcher<Object>) requireNonNull(valueFetcher, "valueFetcher");
      return (B) this;
    }

    @Override
    public final B columnName(String columnName) {
      this.columnName = requireNonNull(columnName, "columnName");
      return (B) this;
    }

    @Override
    public B columnExpression(String columnExpression) {
      this.columnExpression = requireNonNull(columnExpression, "columnExpression");
      return (B) this;
    }

    @Override
    public final B columnHasDefaultValue(boolean columnHasDefaultValue) {
      this.columnHasDefaultValue = columnHasDefaultValue;
      return (B) this;
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
    public final B groupingColumn(boolean groupingColumn) {
      this.groupingColumn = groupingColumn;
      this.aggregateColumn = !groupingColumn;
      return (B) this;
    }

    @Override
    public final B aggregateColumn(boolean aggregateColumn) {
      this.aggregateColumn = aggregateColumn;
      this.groupingColumn = !aggregateColumn;
      return (B) this;
    }

    @Override
    public final B selectable(boolean selectable) {
      this.selectable = selectable;
      return (B) this;
    }

    @Override
    public final B searchProperty(boolean searchProperty) {
      if (searchProperty && !attribute.isString()) {
        throw new IllegalStateException("Search properties must be String based: " + attribute);
      }
      this.searchProperty = searchProperty;
      return (B) this;
    }

    /**
     * Returns the default sql type for the given class.
     * @param clazz the class
     * @return the corresponding sql type
     */
    private static int sqlType(Class<?> clazz) {
      return TYPE_MAP.getOrDefault(requireNonNull(clazz, "clazz"), Types.OTHER);
    }

    private static <T> ValueFetcher<T> valueFetcher(int columnType, Class<T> valueClass) {
      if (columnType == Types.OTHER) {
        return (ValueFetcher<T>) new ObjectFetcher(valueClass);
      }
      if (!VALUE_FETCHERS.containsKey(columnType)) {
        throw new IllegalArgumentException("Unsupported SQL value type: " + columnType +
                ", attribute value class: " + valueClass.getName());
      }

      return (ValueFetcher<T>) VALUE_FETCHERS.get(columnType);
    }
  }

  abstract static class AbstractReadOnlyColumnPropertyBuilder<T, B extends ColumnProperty.Builder<T, B>>
          extends DefaultColumnPropertyBuilder<T, B> implements Property.Builder<T, B> {

    protected AbstractReadOnlyColumnPropertyBuilder(Attribute<T> attribute, String caption) {
      super(attribute, caption);
      super.readOnly(true);
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

  static final class DefaultSubqueryPropertyBuilder<T, B extends ColumnProperty.Builder<T, B>>
          extends AbstractReadOnlyColumnPropertyBuilder<T, B> implements Property.Builder<T, B> {

    DefaultSubqueryPropertyBuilder(Attribute<T> attribute, String caption, String subquery) {
      super(attribute, caption);
      super.columnExpression("(" + subquery + ")");
    }

    @Override
    public B columnExpression(String columnExpression) {
      throw new UnsupportedOperationException("Column expression can not be set on a subquery property: " + attribute);
    }

    @Override
    public Property<T> build() {
      return new DefaultColumnProperty<>(this);
    }
  }

  private static final class ShortFetcher implements ValueFetcher<Short> {

    @Override
    public Short fetchValue(ResultSet resultSet, int index) throws SQLException {
      short value = resultSet.getShort(index);

      return value == 0 && resultSet.wasNull() ? null : value;
    }
  }

  private static final class IntegerFetcher implements ValueFetcher<Integer> {

    @Override
    public Integer fetchValue(ResultSet resultSet, int index) throws SQLException {
      int value = resultSet.getInt(index);

      return value == 0 && resultSet.wasNull() ? null : value;
    }
  }

  private static final class LongFetcher implements ValueFetcher<Long> {

    @Override
    public Long fetchValue(ResultSet resultSet, int index) throws SQLException {
      long value = resultSet.getLong(index);

      return value == 0L && resultSet.wasNull() ? null : value;
    }
  }

  private static final class DoubleFetcher implements ValueFetcher<Double> {

    @Override
    public Double fetchValue(ResultSet resultSet, int index) throws SQLException {
      double value = resultSet.getDouble(index);

      return Double.compare(value, 0d) == 0 && resultSet.wasNull() ? null : value;
    }
  }

  private static final class BigDecimalFetcher implements ValueFetcher<BigDecimal> {

    @Override
    public BigDecimal fetchValue(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getBigDecimal(index);
    }
  }

  private static final class LocalDateFetcher implements ValueFetcher<LocalDate> {

    @Override
    public LocalDate fetchValue(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, LocalDate.class);
    }
  }

  private static final class LocalDateTimeFetcher implements ValueFetcher<LocalDateTime> {

    @Override
    public LocalDateTime fetchValue(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, LocalDateTime.class);
    }
  }

  private static final class OffsetDateTimeFetcher implements ValueFetcher<OffsetDateTime> {

    @Override
    public OffsetDateTime fetchValue(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, OffsetDateTime.class);
    }
  }

  private static final class LocalTimeFetcher implements ValueFetcher<LocalTime> {

    @Override
    public LocalTime fetchValue(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, LocalTime.class);
    }
  }

  private static final class StringFetcher implements ValueFetcher<String> {

    @Override
    public String fetchValue(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getString(index);
    }
  }

  private static final class BooleanFetcher implements ValueFetcher<Boolean> {

    @Override
    public Boolean fetchValue(ResultSet resultSet, int index) throws SQLException {
      boolean value = resultSet.getBoolean(index);

      return !value && resultSet.wasNull() ? null : value;
    }
  }

  private static final class CharacterFetcher implements ValueFetcher<Character> {

    @Override
    public Character fetchValue(ResultSet resultSet, int index) throws SQLException {
      String string = resultSet.getString(index);
      if (nullOrEmpty(string)) {
        return null;
      }

      return Character.valueOf(string.charAt(0));
    }
  }

  private static final class ByteArrayFetcher implements ValueFetcher<byte[]> {

    @Override
    public byte[] fetchValue(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getBytes(index);
    }
  }

  private static final class ObjectFetcher implements ValueFetcher<Object> {

    private final Class<?> valueClass;

    private ObjectFetcher(Class<?> valueClass) {
      this.valueClass = valueClass;
    }

    @Override
    public Object fetchValue(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, valueClass);
    }
  }
}
