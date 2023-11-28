/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.attribute.Column.Converter;
import is.codion.framework.domain.entity.attribute.Column.Fetcher;

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
import java.util.Map;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;

class DefaultColumnDefinition<T> extends AbstractAttributeDefinition<T> implements ColumnDefinition<T> {

  private static final long serialVersionUID = 1;

  private static final Converter<Object, Object> DEFAULT_CONVERTER = new DefaultConverter();
  private static final Map<Class<?>, Integer> TYPE_MAP = createTypeMap();
  private static final Map<Integer, Fetcher<?>> FETCHERS = createFetchers();

  private final int type;
  private final int primaryKeyIndex;
  private final boolean columnHasDefaultValue;
  private final boolean insertable;
  private final boolean updatable;
  private final boolean searchColumn;

  private final transient String name;
  private final transient String expression;
  private final transient Fetcher<Object> fetcher;
  private final transient Converter<T, Object> converter;
  private final transient boolean groupBy;
  private final transient boolean aggregate;
  private final transient boolean selectable;

  protected DefaultColumnDefinition(DefaultColumnDefinitionBuilder<T, ?> builder) {
    super(builder);
    this.type = builder.type;
    this.primaryKeyIndex = builder.primaryKeyIndex;
    this.columnHasDefaultValue = builder.columnHasDefaultValue;
    this.insertable = builder.insertable;
    this.updatable = builder.updatable;
    this.searchColumn = builder.searchColumn;
    this.name = builder.name;
    this.expression = builder.expression == null ? builder.name : builder.expression;
    this.fetcher = builder.fetcher;
    this.converter = builder.converter;
    this.groupBy = builder.groupBy;
    this.aggregate = builder.aggregate;
    this.selectable = builder.selectable;
  }

  @Override
  public final Column<T> attribute() {
    return (Column<T>) super.attribute();
  }

  @Override
  public final String name() {
    return name;
  }

  @Override
  public final String expression() {
    return expression;
  }

  @Override
  public final int type() {
    return type;
  }

  @Override
  public final <C> Converter<C, T> converter() {
    return (Converter<C, T>) converter;
  }

  @Override
  public final boolean columnHasDefaultValue() {
    return columnHasDefaultValue;
  }

  @Override
  public final boolean insertable() {
    return insertable;
  }

  @Override
  public final boolean updatable() {
    return updatable;
  }

  @Override
  public final boolean readOnly() {
    return !insertable && !updatable;
  }

  @Override
  public final int primaryKeyIndex() {
    return primaryKeyIndex;
  }

  @Override
  public final boolean groupBy() {
    return groupBy;
  }

  @Override
  public final boolean aggregate() {
    return aggregate;
  }

  @Override
  public final boolean selectable() {
    return selectable;
  }

  @Override
  public final boolean primaryKey() {
    return primaryKeyIndex >= 0;
  }

  @Override
  public final boolean searchColumn() {
    return searchColumn;
  }

  @Override
  public final T get(ResultSet resultSet, int index) throws SQLException {
    return converter.fromColumnValue(fetcher.get(resultSet, index));
  }

  private static final class DefaultConverter implements Converter<Object, Object> {
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

  private static Map<Integer, Fetcher<?>> createFetchers() {
    Map<Integer, Fetcher<?>> fetchers = new HashMap<>();
    fetchers.put(Types.SMALLINT, new ShortFetcher());
    fetchers.put(Types.INTEGER, new IntegerFetcher());
    fetchers.put(Types.BIGINT, new LongFetcher());
    fetchers.put(Types.DOUBLE, new DoubleFetcher());
    fetchers.put(Types.DECIMAL, new BigDecimalFetcher());
    fetchers.put(Types.DATE, new LocalDateFetcher());
    fetchers.put(Types.TIMESTAMP, new LocalDateTimeFetcher());
    fetchers.put(Types.TIME, new LocalTimeFetcher());
    fetchers.put(Types.TIMESTAMP_WITH_TIMEZONE, new OffsetDateTimeFetcher());
    fetchers.put(Types.TIME_WITH_TIMEZONE, new OffsetTimeFetcher());
    fetchers.put(Types.VARCHAR, new StringFetcher());
    fetchers.put(Types.BOOLEAN, new BooleanFetcher());
    fetchers.put(Types.CHAR, new CharacterFetcher());
    fetchers.put(Types.BLOB, new ByteArrayFetcher());

    return fetchers;
  }

  static class DefaultColumnDefinitionBuilder<T, B extends ColumnDefinition.Builder<T, B>>
          extends AbstractAttributeDefinitionBuilder<T, B> implements ColumnDefinition.Builder<T, B> {

    private final int primaryKeyIndex;

    private int type;
    private boolean columnHasDefaultValue;
    private boolean insertable;
    private boolean updatable;
    private boolean searchColumn;
    private String name;
    private String expression;
    private Fetcher<Object> fetcher;
    private Converter<T, Object> converter;
    private boolean groupBy;
    private boolean aggregate;
    private boolean selectable;

    DefaultColumnDefinitionBuilder(Column<T> column) {
      this(column, -1);
    }

    DefaultColumnDefinitionBuilder(Column<T> column, int primaryKeyIndex) {
      super(column);
      this.primaryKeyIndex = primaryKeyIndex;
      this.type = sqlType(column.type().valueClass());
      this.columnHasDefaultValue = false;
      this.insertable = true;
      nullable(primaryKeyIndex < 0);
      this.updatable = primaryKeyIndex < 0;
      this.searchColumn = false;
      this.name = column.name();
      this.fetcher = (Fetcher<Object>) fetcher(this.type, column);
      this.converter = (Converter<T, Object>) DEFAULT_CONVERTER;
      this.groupBy = false;
      this.aggregate = false;
      this.selectable = true;
    }

    @Override
    public AttributeDefinition<T> build() {
      return new DefaultColumnDefinition<>(this);
    }

    @Override
    public final B nullable(boolean nullable) {
      return super.nullable(nullable);
    }

    @Override
    public final <C> B columnClass(Class<C> columnClass, Converter<T, C> converter) {
      this.type = sqlType(columnClass);
      this.converter = (Converter<T, Object>) requireNonNull(converter, "valueConverter");
      this.fetcher = fetcher(this.type, (Column<Object>) attribute);
      return (B) this;
    }

    @Override
    public final <C> B columnClass(Class<C> columnClass, Converter<T, C> converter,
                                   Fetcher<C> fetcher) {
      this.type = sqlType(columnClass);
      this.converter = (Converter<T, Object>) requireNonNull(converter, "valueConverter");
      this.fetcher = (Fetcher<Object>) requireNonNull(fetcher, "valueFetcher");
      return (B) this;
    }

    @Override
    public final B name(String name) {
      this.name = requireNonNull(name, "name");
      return (B) this;
    }

    @Override
    public B expression(String expression) {
      this.expression = requireNonNull(expression, "expression");
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

    /**
     * Returns the default sql type for the given class.
     * @param clazz the class
     * @return the corresponding sql type
     */
    private static int sqlType(Class<?> clazz) {
      return TYPE_MAP.getOrDefault(requireNonNull(clazz, "clazz"), Types.OTHER);
    }

    private static <T> Fetcher<T> fetcher(int columnType, Column<T> column) {
      if (columnType == Types.OTHER) {
        return (Fetcher<T>) new ObjectFetcher(column.type().valueClass());
      }
      if (!FETCHERS.containsKey(columnType)) {
        throw new IllegalArgumentException("Unsupported SQL value type: " + columnType +
                ", column: " + column + ", valueClass: " + column.type().valueClass());
      }

      return (Fetcher<T>) FETCHERS.get(columnType);
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
      super.expression("(" + subquery + ")");
    }

    @Override
    public B expression(String expression) {
      throw new UnsupportedOperationException("Column expression can not be set on a subquery column: " + attribute);
    }
  }

  private static final class ShortFetcher implements Fetcher<Short> {

    @Override
    public Short get(ResultSet resultSet, int index) throws SQLException {
      short value = resultSet.getShort(index);

      return value == 0 && resultSet.wasNull() ? null : value;
    }
  }

  private static final class IntegerFetcher implements Fetcher<Integer> {

    @Override
    public Integer get(ResultSet resultSet, int index) throws SQLException {
      int value = resultSet.getInt(index);

      return value == 0 && resultSet.wasNull() ? null : value;
    }
  }

  private static final class LongFetcher implements Fetcher<Long> {

    @Override
    public Long get(ResultSet resultSet, int index) throws SQLException {
      long value = resultSet.getLong(index);

      return value == 0L && resultSet.wasNull() ? null : value;
    }
  }

  private static final class DoubleFetcher implements Fetcher<Double> {

    @Override
    public Double get(ResultSet resultSet, int index) throws SQLException {
      double value = resultSet.getDouble(index);

      return Double.compare(value, 0d) == 0 && resultSet.wasNull() ? null : value;
    }
  }

  private static final class BigDecimalFetcher implements Fetcher<BigDecimal> {

    @Override
    public BigDecimal get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getBigDecimal(index);
    }
  }

  private static final class LocalDateFetcher implements Fetcher<LocalDate> {

    @Override
    public LocalDate get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, LocalDate.class);
    }
  }

  private static final class LocalDateTimeFetcher implements Fetcher<LocalDateTime> {

    @Override
    public LocalDateTime get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, LocalDateTime.class);
    }
  }

  private static final class OffsetDateTimeFetcher implements Fetcher<OffsetDateTime> {

    @Override
    public OffsetDateTime get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, OffsetDateTime.class);
    }
  }

  private static final class LocalTimeFetcher implements Fetcher<LocalTime> {

    @Override
    public LocalTime get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, LocalTime.class);
    }
  }

  private static final class OffsetTimeFetcher implements Fetcher<OffsetTime> {

    @Override
    public OffsetTime get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getObject(index, OffsetTime.class);
    }
  }

  private static final class StringFetcher implements Fetcher<String> {

    @Override
    public String get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getString(index);
    }
  }

  private static final class BooleanFetcher implements Fetcher<Boolean> {

    @Override
    public Boolean get(ResultSet resultSet, int index) throws SQLException {
      boolean value = resultSet.getBoolean(index);

      return !value && resultSet.wasNull() ? null : value;
    }
  }

  private static final class CharacterFetcher implements Fetcher<Character> {

    @Override
    public Character get(ResultSet resultSet, int index) throws SQLException {
      String string = resultSet.getString(index);
      if (nullOrEmpty(string)) {
        return null;
      }

      return Character.valueOf(string.charAt(0));
    }
  }

  private static final class ByteArrayFetcher implements Fetcher<byte[]> {

    @Override
    public byte[] get(ResultSet resultSet, int index) throws SQLException {
      return resultSet.getBytes(index);
    }
  }

  private static final class ObjectFetcher implements Fetcher<Object> {

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
