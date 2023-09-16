/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.attribute.Column.ValueConverter;
import is.codion.framework.domain.entity.attribute.Column.ValueFetcher;

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
import java.time.OffsetTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;

class DefaultColumnDefinition<T> extends AbstractAttributeDefinition<T> implements ColumnDefinition<T> {

  private static final long serialVersionUID = 1;

  private static final ValueConverter<Object, Object> DEFAULT_VALUE_CONVERTER = new DefaultValueConverter();
  private static final Map<Class<?>, Integer> TYPE_MAP = createTypeMap();
  private static final Map<Integer, ValueFetcher<?>> VALUE_FETCHERS = createValueFetchers();

  private final int columnType;
  private final int primaryKeyIndex;
  private final boolean columnHasDefaultValue;
  private final boolean insertable;
  private final boolean updatable;
  private final boolean searchColumn;

  private final transient String columnName;
  private final transient String columnExpression;
  private final transient ValueFetcher<Object> valueFetcher;
  private final transient ValueConverter<T, Object> valueConverter;
  private final transient boolean groupBy;
  private final transient boolean aggregate;
  private final transient boolean selectable;

  protected DefaultColumnDefinition(DefaultColumnDefinitionBuilder<T, ?> builder) {
    super(builder);
    this.columnType = builder.columnType;
    this.primaryKeyIndex = builder.primaryKeyIndex;
    this.columnHasDefaultValue = builder.columnHasDefaultValue;
    this.insertable = builder.insertable;
    this.updatable = builder.updatable;
    this.searchColumn = builder.searchColumn;
    this.columnName = builder.columnName;
    this.columnExpression = builder.columnExpression;
    this.valueFetcher = builder.valueFetcher;
    this.valueConverter = builder.valueConverter;
    this.groupBy = builder.groupBy;
    this.aggregate = builder.aggregate;
    this.selectable = builder.selectable;
  }

  @Override
  public final Column<T> attribute() {
    return (Column<T>) super.attribute();
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
  public final <C> ValueConverter<C, T> valueConverter() {
    return (ValueConverter<C, T>) valueConverter;
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
  public final int primaryKeyIndex() {
    return primaryKeyIndex;
  }

  @Override
  public final boolean isGroupBy() {
    return groupBy;
  }

  @Override
  public final boolean isAggregate() {
    return aggregate;
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
  public final boolean isSearchColumn() {
    return searchColumn;
  }

  @Override
  public final T get(ResultSet resultSet, int index) throws SQLException {
    return valueConverter.fromColumnValue(valueFetcher.get(resultSet, index));
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
    typeMap.put(OffsetTime.class, Types.TIME_WITH_TIMEZONE);
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
    valueFetchers.put(Types.TIME, new LocalTimeFetcher());
    valueFetchers.put(Types.TIMESTAMP_WITH_TIMEZONE, new OffsetDateTimeFetcher());
    valueFetchers.put(Types.TIME_WITH_TIMEZONE, new OffsetTimeFetcher());
    valueFetchers.put(Types.VARCHAR, new StringFetcher());
    valueFetchers.put(Types.BOOLEAN, new BooleanFetcher());
    valueFetchers.put(Types.CHAR, new CharacterFetcher());
    valueFetchers.put(Types.BLOB, new ByteArrayFetcher());

    return valueFetchers;
  }

  static class DefaultColumnDefinitionBuilder<T, B extends ColumnDefinition.Builder<T, B>>
          extends AbstractAttributeDefinitionBuilder<T, B> implements ColumnDefinition.Builder<T, B> {

    private int columnType;
    private int primaryKeyIndex;
    private boolean columnHasDefaultValue;
    private boolean insertable;
    private boolean updatable;
    private boolean searchColumn;
    private String columnName;
    private String columnExpression;
    private ValueFetcher<Object> valueFetcher;
    private ValueConverter<T, Object> valueConverter;
    private boolean groupBy;
    private boolean aggregate;
    private boolean selectable;

    List<Item<T>> items;

    DefaultColumnDefinitionBuilder(Column<T> column) {
      super(column);
      this.primaryKeyIndex = -1;
      this.columnType = sqlType(column.type().valueClass());
      this.columnHasDefaultValue = false;
      this.insertable = true;
      this.updatable = true;
      this.searchColumn = false;
      this.columnName = column.name();
      this.valueFetcher = (ValueFetcher<Object>) valueFetcher(this.columnType, column);
      this.valueConverter = (ValueConverter<T, Object>) DEFAULT_VALUE_CONVERTER;
      this.groupBy = false;
      this.aggregate = false;
      this.selectable = true;
    }

    @Override
    public AttributeDefinition<T> build() {
      if (items != null) {
        return new DefaultItemColumnDefinition<>(this);
      }

      return new DefaultColumnDefinition<>(this);
    }

    @Override
    public final <C> B columnClass(Class<C> columnClass, ValueConverter<T, C> valueConverter) {
      this.columnType = sqlType(columnClass);
      this.valueConverter = (ValueConverter<T, Object>) requireNonNull(valueConverter, "valueConverter");
      this.valueFetcher = valueFetcher(this.columnType, (Column<Object>) attribute);
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
    public final B primaryKeyIndex(int primaryKeyIndex) {
      if (primaryKeyIndex < 0) {
        throw new IllegalArgumentException("Primary key index must be at least 0: " + attribute);
      }
      this.primaryKeyIndex = primaryKeyIndex;
      nullable(false);
      updatable(false);
      return (B) this;
    }

    @Override
    public final B groupBy(boolean groupBy) {
      this.groupBy = groupBy;
      this.aggregate = !groupBy;
      return (B) this;
    }

    @Override
    public final B aggregate(boolean aggregate) {
      this.aggregate = aggregate;
      this.groupBy = !aggregate;
      return (B) this;
    }

    @Override
    public final B selectable(boolean selectable) {
      this.selectable = selectable;
      return (B) this;
    }

    @Override
    public final B searchColumn(boolean searchColumn) {
      if (searchColumn && !attribute.type().isString()) {
        throw new IllegalStateException("Search columns must be String based: " + attribute);
      }
      this.searchColumn = searchColumn;
      return (B) this;
    }

    @Override
    public final B items(List<Item<T>> items) {
      this.items = validateItems(items);
      return (B) this;
    }

    private static <T> List<Item<T>> validateItems(List<Item<T>> items) {
      if (requireNonNull(items).size() != items.stream()
              .map(new GetItemValue<>())
              .collect(Collectors.toSet())
              .size()) {
        throw new IllegalArgumentException("Item list contains duplicate values: " + items);
      }

      return items;
    }

    /**
     * Returns the default sql type for the given class.
     * @param clazz the class
     * @return the corresponding sql type
     */
    private static int sqlType(Class<?> clazz) {
      return TYPE_MAP.getOrDefault(requireNonNull(clazz, "clazz"), Types.OTHER);
    }

    private static <T> ValueFetcher<T> valueFetcher(int columnType, Column<T> column) {
      if (columnType == Types.OTHER) {
        return (ValueFetcher<T>) new ObjectFetcher(column.type().valueClass());
      }
      if (!VALUE_FETCHERS.containsKey(columnType)) {
        throw new IllegalArgumentException("Unsupported SQL value type: " + columnType +
                ", column: " + column + ", valueClass: " + column.type().valueClass());
      }

      return (ValueFetcher<T>) VALUE_FETCHERS.get(columnType);
    }

    private static final class GetItemValue<T> implements Function<Item<T>, T> {

      @Override
      public T apply(Item<T> item) {
        return item.get();
      }
    }
  }

  abstract static class AbstractReadOnlyColumnDefinitionBuilder<T, B extends ColumnDefinition.Builder<T, B>>
          extends DefaultColumnDefinitionBuilder<T, B> implements AttributeDefinition.Builder<T, B> {

    protected AbstractReadOnlyColumnDefinitionBuilder(Column<T> column) {
      super(column);
      super.readOnly(true);
    }

    @Override
    public final B readOnly(boolean readOnly) {
      throw new UnsupportedOperationException("Read only by default: " + attribute);
    }

    @Override
    public final B insertable(boolean insertable) {
      throw new UnsupportedOperationException("Column is not insertable: " + attribute);
    }

    @Override
    public final B updatable(boolean updatable) {
      throw new UnsupportedOperationException("Column is not updatable: " + attribute);
    }
  }

  static final class DefaultSubqueryColumnDefinitionBuilder<T, B extends ColumnDefinition.Builder<T, B>>
          extends AbstractReadOnlyColumnDefinitionBuilder<T, B> implements AttributeDefinition.Builder<T, B> {

    DefaultSubqueryColumnDefinitionBuilder(Column<T> column, String subquery) {
      super(column);
      super.columnExpression("(" + subquery + ")");
    }

    @Override
    public B columnExpression(String columnExpression) {
      throw new UnsupportedOperationException("Column expression can not be set on a subquery column: " + attribute);
    }
  }

  private static final class ShortFetcher implements ValueFetcher<Short> {

    @Override
    public Short get(ResultSet resultSet, int index) throws SQLException {
      short value = resultSet.getShort(index);

      return value == 0 && resultSet.wasNull() ? null : value;
    }
  }

  private static final class IntegerFetcher implements ValueFetcher<Integer> {

    @Override
    public Integer get(ResultSet resultSet, int index) throws SQLException {
      int value = resultSet.getInt(index);

      return value == 0 && resultSet.wasNull() ? null : value;
    }
  }

  private static final class LongFetcher implements ValueFetcher<Long> {

    @Override
    public Long get(ResultSet resultSet, int index) throws SQLException {
      long value = resultSet.getLong(index);

      return value == 0L && resultSet.wasNull() ? null : value;
    }
  }

  private static final class DoubleFetcher implements ValueFetcher<Double> {

    @Override
    public Double get(ResultSet resultSet, int index) throws SQLException {
      double value = resultSet.getDouble(index);

      return Double.compare(value, 0d) == 0 && resultSet.wasNull() ? null : value;
    }
  }

  private static final class BigDecimalFetcher implements ValueFetcher<BigDecimal> {

    @Override
    public BigDecimal get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getBigDecimal(index);
    }
  }

  private static final class LocalDateFetcher implements ValueFetcher<LocalDate> {

    @Override
    public LocalDate get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, LocalDate.class);
    }
  }

  private static final class LocalDateTimeFetcher implements ValueFetcher<LocalDateTime> {

    @Override
    public LocalDateTime get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, LocalDateTime.class);
    }
  }

  private static final class OffsetDateTimeFetcher implements ValueFetcher<OffsetDateTime> {

    @Override
    public OffsetDateTime get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, OffsetDateTime.class);
    }
  }

  private static final class LocalTimeFetcher implements ValueFetcher<LocalTime> {

    @Override
    public LocalTime get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, LocalTime.class);
    }
  }

  private static final class OffsetTimeFetcher implements ValueFetcher<OffsetTime> {

    @Override
    public OffsetTime get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, OffsetTime.class);
    }
  }

  private static final class StringFetcher implements ValueFetcher<String> {

    @Override
    public String get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getString(index);
    }
  }

  private static final class BooleanFetcher implements ValueFetcher<Boolean> {

    @Override
    public Boolean get(ResultSet resultSet, int index) throws SQLException {
      boolean value = resultSet.getBoolean(index);

      return !value && resultSet.wasNull() ? null : value;
    }
  }

  private static final class CharacterFetcher implements ValueFetcher<Character> {

    @Override
    public Character get(ResultSet resultSet, int index) throws SQLException {
      String string = resultSet.getString(index);
      if (nullOrEmpty(string)) {
        return null;
      }

      return Character.valueOf(string.charAt(0));
    }
  }

  private static final class ByteArrayFetcher implements ValueFetcher<byte[]> {

    @Override
    public byte[] get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getBytes(index);
    }
  }

  private static final class ObjectFetcher implements ValueFetcher<Object> {

    private final Class<?> valueClass;

    private ObjectFetcher(Class<?> valueClass) {
      this.valueClass = valueClass;
    }

    @Override
    public Object get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, valueClass);
    }
  }
}
