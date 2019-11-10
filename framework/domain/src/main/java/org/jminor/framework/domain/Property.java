/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Configuration;
import org.jminor.common.Item;
import org.jminor.common.PropertyValue;
import org.jminor.common.db.ResultPacker;

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
   * @return the bean property name associated with this property
   */
  String getBeanProperty();

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
  boolean isTemporal();

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
   * @return the maximum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  Double getMax();

  /**
   * @return the minimum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  Double getMin();

  /**
   * @return the maximum number of fraction digits to use for this property value,
   * only applicable to properties based on Types.DOUBLE and Types.DECIMAL
   */
  int getMaximumFractionDigits();

  /**
   * @return the preferred column width of this property in pixels when presented in a table, 0 if none has been specified
   */
  int getPreferredColumnWidth();

  /**
   * @return true if values associated with this property can be set null
   */
  boolean isNullable();

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
   * @return the Format object used to format the value of properties when being presented
   */
  Format getFormat();

  /**
   * @return the date/time format pattern
   */
  String getDateTimeFormatPattern();

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
     * @return this propertys zero based index in the primary key, -1 if this property is not part of a primary key
     */
    int getPrimaryKeyIndex();

    /**
     * @return true if this property is part of a primary key
     */
    boolean isPrimaryKeyProperty();

    /**
     * @return true if this column is a group by column
     */
    boolean isGroupingColumn();

    /**
     * @return true if this is an aggregate column
     */
    boolean isAggregateColumn();

    /**
     * @return true if this property should be included in select queries
     */
    boolean isSelectable();

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
     * @return true if this foreign key is not based on a physical (table) foreign key
     * and should not prevent deletion
     */
    boolean isSoftReference();

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
