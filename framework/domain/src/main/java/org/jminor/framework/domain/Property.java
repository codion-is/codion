/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Configuration;
import org.jminor.common.Item;
import org.jminor.common.PropertyValue;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.ValueConverter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Format;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Specifies a Property.
 */
public interface Property extends Serializable {

  int DEFAULT_MAXIMUM_FRACTION_DIGITS = 10;
  int DEFAULT_FOREIGN_KEY_FETCH_DEPTH = 1;

  /**
   * Specifies the default maximum number of fraction digits for double property values<br>
   * Note that values are rounded when set.<br>
   * Value type: Integer<br>
   * Default value: 10
   */
  PropertyValue<Integer> MAXIMUM_FRACTION_DIGITS = Configuration.integerValue("jminor.domain.maximumFractionDigits", DEFAULT_MAXIMUM_FRACTION_DIGITS);

  /**
   * Specifies the rounding mode used for BigDecimal property values<br>
   * Value type: Integer<br>
   * Default value: BigDecimal.ROUND_HALF_EVEN<br>
   * @see #MAXIMUM_FRACTION_DIGITS
   */
  PropertyValue<Integer> BIG_DECIMAL_ROUNDING_MODE = Configuration.integerValue("jminor.domain.bigDecimalRoundingMode", BigDecimal.ROUND_HALF_EVEN);

  /**
   * The date format pattern to use when showing time values in tables and when creating default time input fields<br>
   * Value type: String<br>
   * Default value: HH:mm
   */
  PropertyValue<String> TIME_FORMAT = Configuration.stringValue("jminor.domain.timeFormat", "HH:mm");

  /**
   * The date format pattern to use when showing timestamp values in tables and when creating default timestamp input fields<br>
   * Value type: String<br>
   * Default value: dd-MM-yyyy HH:mm
   */
  PropertyValue<String> TIMESTAMP_FORMAT = Configuration.stringValue("jminor.domain.timestampFormat", "dd-MM-yyyy HH:mm");

  /**
   * The date format pattern to use when showing date values in tables and when creating default date input fields<br>
   * Value type: String<br>
   * Default value: dd-MM-yyyy
   */
  PropertyValue<String> DATE_FORMAT = Configuration.stringValue("jminor.domain.dateFormat", "dd-MM-yyyy");

  /**
   * Specifies the default foreign key fetch depth<br>
   * Value type: Integer<br>
   * Default value: 1
   */
  PropertyValue<Integer> FOREIGN_KEY_FETCH_DEPTH = Configuration.integerValue("jminor.domain.foreignKeyFetchDepth", DEFAULT_FOREIGN_KEY_FETCH_DEPTH);

  /**
   * Specifies the wildcard character used by the framework<br>
   * Value type: String<br>
   * Default value: %
   */
  PropertyValue<String> WILDCARD_CHARACTER = Configuration.stringValue("jminor.wildcardCharacter", "%");

  /**
   * @return the id of the entity this property is associated with
   */
  String getEntityId();

  /**
   * @param entityId the id of the entity this property is associated with
   * @throws IllegalStateException in case the entityId has already been set
   * @return this Property instance
   */
  Property setEntityId(final String entityId);

  /**
   * The property identifier, should be unique within an Entity.
   * By default this id serves as column name for database properties.
   * @return the id of this property
   */
  String getPropertyId();

  /**
   * @return the caption
   */
  String getCaption();

  /**
   * @return a String describing this attribute
   */
  String getDescription();

  /**
   * @return the Class representing the values of this attribute
   */
  Class getTypeClass();

  /**
   * @param value the value to validate
   * @throws IllegalArgumentException in case {@code value} is of a type incompatible with this attribute
   */
  void validateType(final Object value);

  /**
   * @param propertyId the property ID
   * @return true if this property is of the given type
   */
  boolean is(final String propertyId);

  /**
   * @param property the property
   * @return true if this property is of the given type
   */
  boolean is(final Property property);

  /**
   * @return the data type ({@link java.sql.Types}) of the value of this property
   */
  int getType();

  /**
   * @param type the type to check ({@link java.sql.Types})
   * @return true if the type of this property is the one given
   */
  boolean isType(final int type);

  /**
   * @return true if this is a numerical Property, that is, Integer or Double
   */
  boolean isNumerical();

  /**
   * @return true if this is a time based property, Date (LocalDate), Timestamp (LocalDatetime) or Time (LocalTime)
   */
  boolean isDateOrTime();

  /**
   * @return true if this is a date property
   */
  boolean isDate();

  /**
   * @return true if this is a timestamp property
   */
  boolean isTimestamp();

  /**
   * @return true if this is a time property
   */
  boolean isTime();

  /**
   * @return true if this is a character property
   */
  boolean isCharacter();

  /**
   * @return true if this is a string property
   */
  boolean isString();

  /**
   * @return true if this is a long property
   */
  boolean isLong();

  /**
   * @return true if this is a integer property
   */
  boolean isInteger();

  /**
   * @return true if this is a double property
   */
  boolean isDouble();

  /**
   * @return true if this is a BigDecimal property
   */
  boolean isBigDecimal();

  /**
   * @return true if this is a decimal property
   */
  boolean isDecimal();

  /**
   * @return true if this is a boolean property
   */
  boolean isBoolean();

  /**
   * Sets the default value for this property, overrides the underlying column default value, if any
   * @param defaultValue the value to use as default
   * @return this Property instance
   */
  Property setDefaultValue(final Object defaultValue);

  /**
   * Sets the default value provider, use in case of dynamic default values.
   * @param provider the default value provider
   * @return this Property instance
   */
  Property setDefaultValueProvider(final ValueProvider provider);

  /**
   * @return true if a default value has been set for this property
   */
  boolean hasDefaultValue();

  /**
   * @return the default value for this property, if no default value has been set null is returned
   * @see #hasDefaultValue()
   */
  Object getDefaultValue();

  /**
   * @return true if this property should be hidden in table views
   */
  boolean isHidden();

  /**
   * @param hidden specifies whether this property should hidden in table views
   * @return this Property instance
   */
  Property setHidden(final boolean hidden);

  /**
   * @return the maximum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  Double getMax();

  /**
   * Sets the maximum allowed value for this property, only applicable to numerical properties
   * @param max the maximum allowed value
   * @return this Property instance
   */
  Property setMax(final double max);

  /**
   * @return the minimum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  Double getMin();

  /**
   * Only applicable to numerical properties
   * @param min the minimum allowed value for this property
   * @return this Property instance
   */
  Property setMin(final double min);

  /**
   * Sets the maximum fraction digits to show for this property, only applicable to properties based on Types.DOUBLE.
   * This setting is overridden during subsequent calls to {@link #setFormat(java.text.Format)}.
   * Note that values associated with this property are automatically rounded to {@code maximumFractionDigits} digits.
   * @param maximumFractionDigits the maximum fraction digits
   * @return this Property instance
   */
  Property setMaximumFractionDigits(final int maximumFractionDigits);

  /**
   * @return the maximum number of fraction digits to use for this property value,
   * only applicable to properties based on Types.DOUBLE and Types.DECIMAL
   */
  int getMaximumFractionDigits();

  /**
   * Specifies whether to use number grouping when presenting the value associated with this property.
   * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale.
   * By default grouping is not used.
   * Only applicable to numerical properties.
   * This setting is overridden during subsequent calls to {@code setFormat}
   * @param useGrouping if true then number grouping is used
   * @return this Property instance
   */
  Property setUseNumberFormatGrouping(final boolean useGrouping);

  /**
   * @return the preferred column width of this property in pixels when presented in a table, 0 if none has been specified
   */
  int getPreferredColumnWidth();

  /**
   * @param preferredColumnWidth the preferred column width of this property in pixels when displayed in a table
   * @return this Property instance
   */
  Property setPreferredColumnWidth(final int preferredColumnWidth);

  /**
   * @param readOnly specifies whether this property should be included during insert/update operations
   * @return this Property instance
   */
  Property setReadOnly(final boolean readOnly);

  /**
   * Specifies whether or not this property is nullable, in case of
   * properties that are parts of a ForeignKeyProperty inherit the nullable state of that property.
   * @param nullable specifies whether or not this property accepts a null value
   * @return this Property instance
   */
  Property setNullable(final boolean nullable);

  /**
   * @return true if values associated with this property can be set null
   */
  boolean isNullable();

  /**
   * Sets the maximum length of this property value, this applies to String (varchar) based properties
   * @param maxLength the maximum length
   * @return this Property instance
   */
  Property setMaxLength(final int maxLength);

  /**
   * @return the maximum length of this property value, -1 is returned if the max length is undefined,
   * this applies to String (varchar) based properties
   */
  int getMaxLength();

  /**
   * @return the mnemonic to use when creating a label for this property
   */
  Character getMnemonic();

  /**
   * Sets the mnemonic to use when creating a label for this property
   * @param mnemonic the mnemonic character
   * @return this Property instance
   */
  Property setMnemonic(final Character mnemonic);

  /**
   * @param description a String describing this property
   * @return this Property instance
   */
  Property setDescription(final String description);

  /**
   * @return the Format object used to format the value of properties when being presented
   */
  Format getFormat();

  /**
   * Sets the Format to use when presenting property values
   * @param format the format to use
   * @return this Property instance
   * @throws NullPointerException in case format is null
   * @throws IllegalArgumentException in case the format does not fit the property type,
   * f.ex. NumberFormat is expected for numerical properties
   */
  Property setFormat(final Format format);

  /**
   * @return the date/time format pattern
   */
  String getDateTimeFormatPattern();

  /**
   * Sets the date/time format pattern used when presenting values
   * @param dateTimeFormatPattern the format pattern
   * @return this Property instance
   * @throws IllegalArgumentException in case the pattern is invalid or if this property is not a date/time based one
   */
  Property setDateTimeFormatPattern(final String dateTimeFormatPattern);

  /**
   * @return the DateTimeFormatter for this property or null if this is not a date/time based property
   */
  DateTimeFormatter getDateTimeFormatter();

  /**
   * Specifies whether or not this attribute is read only
   * @return true if this attribute is read only
   */
  boolean isReadOnly();

  /**
   * Specifies a property based on a table column
   */
  interface ColumnProperty extends Property {

    /**
     * @return the column name
     */
    String getColumnName();

    /**
     * @return the data type of the underlying column, usually the same as {@link #getType()}
     * but can differ when the database system does not have native support for the given data type,
     * such as boolean
     */
    int getColumnType();

    /**
     * Sets the actual string used as column when querying
     * @param columnName the column name
     * @return this Property instance
     */
    ColumnProperty setColumnName(final String columnName);

    /**
     * Translates the given value into a sql value, usually this is not required
     * but for certain types this may be necessary, such as boolean values
     * represented by a non-boolean data type in the underlying database
     * @param value the value to translate
     * @return the sql value used to represent the given value
     */
    Object toColumnValue(final Object value);

    /**
     * @param value the SQL value Object to translate from
     * @return the value of SQL {@code value}
     */
    Object fromColumnValue(final Object value);

    /**
     * @param updatable specifies whether this property is updatable
     * @return this Property instance
     */
    ColumnProperty setUpdatable(final boolean updatable);

    /**
     * @param columnHasDefaultValue specifies whether or not the underlying column has a default value
     * @return this Property instance
     */
    ColumnProperty setColumnHasDefaultValue(final boolean columnHasDefaultValue);

    /**
     * @return this propertys zero based index in the primary key, -1 if this property is not part of a primary key
     */
    int getPrimaryKeyIndex();

    /**
     * Sets the zero based primary key index of this property.
     * Note that setting the primary key index renders this property non-null and non-updatable by default,
     * these can be reverted by setting it as updatable after setting the primary key index.
     * @param index the zero based index
     * @return this ColumnProperty instance
     * @throws IllegalArgumentException in case index is a negative number
     * @see #setNullable(boolean)
     * @see #setUpdatable(boolean)
     */
    ColumnProperty setPrimaryKeyIndex(final int index);

    /**
     * @return true if this property is part of a primary key
     */
    boolean isPrimaryKeyProperty();

    /**
     * @param groupingColumn true if this column should be used in a group by clause
     * @throws IllegalStateException in case the column has already been defined as an aggregate column
     * @return this Property instance
     */
    ColumnProperty setGroupingColumn(final boolean groupingColumn);

    /**
     * @return true if this column is a group by column
     */
    boolean isGroupingColumn();

    /**
     * @param aggregateColumn true if this column is an aggregate function column
     * @throws IllegalStateException in case the column has already been defined as a grouping column
     * @return this Property instance
     */
    ColumnProperty setAggregateColumn(final boolean aggregateColumn);

    /**
     * @return true if this is an aggregate column
     */
    boolean isAggregateColumn();

    /**
     * Indicates whether or not this column is updatable
     * @return true if this column is updatable
     */
    boolean isUpdatable();

    /**
     * @return true if this column is a denormalized column, one should which receives a value
     * from a column in a table referenced via a foreign key
     */
    boolean isDenormalized();

    /**
     * @return true if this property is part of a ForeignKeyProperty
     */
    boolean isForeignKeyProperty();

    /**
     * @param foreignKeyProperty the ForeignKeyProperty this property is part of
     */
    void setForeignKeyProperty(final ForeignKeyProperty foreignKeyProperty);

    /**
     * @return the ForeignKeyProperty this property is part of, if any
     */
    ForeignKeyProperty getForeignKeyProperty();

    /**
     * @return true if the underlying column has a default value
     */
    boolean columnHasDefaultValue();

    /**
     * Fetches a value for this property from a ResultSet
     * @param resultSet the ResultSet
     * @param index the index of the column to fetch
     * @return a single value fetched from the given ResultSet
     * @throws java.sql.SQLException in case of an exception
     */
    Object fetchValue(final ResultSet resultSet, final int index) throws SQLException;

    /**
     * Set a value converter, for converting to and from a sql representation of the value
     * @param valueConverter the converter
     * @return this Property instance
     */
    ColumnProperty setValueConverter(final ValueConverter<?, ?> valueConverter);

    /**
     * @return a ResultPacker responsible for packing this property
     */
    ResultPacker<Object> getResultPacker();
  }

  /**
   * A wrapper property that represents a reference to another entity, typically but not necessarily based on a foreign key.
   * These do not map directly to a underlying table column, but wrap the actual column properties involved in the relation.
   * e.g.: Properties.foreignKeyProperty("reference_fk", Properties.columnProperty("reference_id")), where "reference_id" is the
   * actual name of the column involved in the reference, but "reference_fk" is simply a descriptive property ID
   */
  interface ForeignKeyProperty extends Property {

    /**
     * @return true if all reference properties comprising this
     * foreign key property are updatable
     */
    boolean isUpdatable();

    /**
     * @return the id of the entity referenced by this foreign key
     */
    String getForeignEntityId();

    /**
     * Returns an unmodifiable list containing the properties that comprise this foreign key
     * @return the reference properties
     */
    List<ColumnProperty> getProperties();

    /**
     * @return true if this foreign key is based on multiple columns
     */
    boolean isCompositeKey();

    /**
     * @return the default query fetch depth for this foreign key
     */
    int getFetchDepth();

    /**
     * @param fetchDepth the default query fetch depth for this foreign key
     * @return this ForeignKeyProperty instance
     */
    ForeignKeyProperty setFetchDepth(final int fetchDepth);

    /**
     * @return true if this foreign key is not based on a physical (table) foreign key
     * and should not prevent deletion
     */
    boolean isSoftReference();

    /**
     * @param softReference true if this foreign key is not based on a physical (table) foreign key
     * and should not prevent deletion
     * @return this ForeignKeyProperty instance
     */
    ForeignKeyProperty setSoftReference(final boolean softReference);
  }

  /**
   * Represents a property which is part of a composite foreign key but is already included as part of another composite foreign key,
   * and should not handle updating the underlying property, useful in rare cases where multiple foreign keys are referencing tables
   * having composite natural primary keys, using the same column.
   * todo example pleeeeaaase!
   */
  interface MirrorProperty extends ColumnProperty {}

  /**
   * A property representing a column that should get its value automatically from a column in a referenced table
   */
  interface DenormalizedProperty extends ColumnProperty {

    /**
     * @return the id of the foreign key property from which this property should retrieve its value
     */
    String getForeignKeyPropertyId();

    /**
     * @return the property in the referenced entity from which this property gets its value
     */
    Property getDenormalizedProperty();
  }

  /**
   * A property based on a list of values, each with a displayable caption.
   */
  interface ValueListProperty extends ColumnProperty {

    /**
     * @param value the value to validate
     * @return true if the given value is valid for this property
     */
    boolean isValid(final Object value);

    /**
     * @return an unmodifiable view of the available values
     */
    List<Item> getValues();

    /**
     * @param value the value
     * @return the caption associated with the given value
     */
    String getCaption(final Object value);
  }

  /**
   * A property that does not map to an underlying database column. The value of a transient property
   * is initialized to null when entities are loaded, which means transient properties always have null as the original value.
   * The value of transient properties can be set and retrieved like normal properties but are ignored during DML operations.
   * Note that by default setting a transient value marks the entity as being modified, but trying to update an entity
   * with only transient values modified will result in an error.
   */
  interface TransientProperty extends Property {

    /**
     * @param modifiesEntity if true then modifications to the value result in the owning entity becoming modified
     * @return this property instance
     */
    TransientProperty setModifiesEntity(final boolean modifiesEntity);

    /**
     * @return true if the value of this property being modified should result in a modified entity
     */
    boolean isModifiesEntity();
  }

  /**
   * A property which value is derived from the values of one or more properties.
   */
  interface DerivedProperty extends TransientProperty {

    /**
     * @return the ids of properties this property derives from.
     */
    List<String> getSourcePropertyIds();

    /**
     * @return the value provider, providing the derived value
     */
    Provider getValueProvider();

    /**
     * Responsible for providing values derived from other values
     */
    interface Provider extends Serializable {

      /**
       * @param sourceValues the source values, mapped to their respective propertyIds
       * @return the derived value
       */
      Object getValue(final Map<String, Object> sourceValues);
    }
  }

  /**
   * A property based on a subquery, returning a single value
   */
  interface SubqueryProperty extends ColumnProperty {

    /**
     * @return the subquery string
     */
    String getSubQuery();
  }

  /**
   * A property representing an audit column
   */
  interface AuditProperty extends ColumnProperty {

    /**
     * The possible audit actions
     */
    enum AuditAction {
      INSERT, UPDATE
    }

    /**
     * @return the audit action this property represents
     */
    AuditAction getAuditAction();
  }

  /**
   * Specifies a audit property with a timestamp value
   */
  interface AuditTimeProperty extends AuditProperty {}

  /**
   * Specifies a audit property with a username value
   */
  interface AuditUserProperty extends AuditProperty {}

  /**
   * Provides a single value
   */
  interface ValueProvider extends Serializable {
    /**
     * @return the provided value
     */
    Object getValue();
  }
}
