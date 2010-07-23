/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.Column;
import org.jminor.common.model.Attribute;
import org.jminor.common.model.Item;

import java.text.Format;
import java.util.Collection;
import java.util.List;

public interface Property extends Attribute {

  /**
   * @param property the property
   * @return true if this property is of the given type
   */
  boolean is(final Property property);


  /**
   * @return true if this is a numerical Property, that is, Integer or Double
   */
  boolean isNumerical();

  /**
   * @return true if this is a time based property, Date or Timestamp
   */
  boolean isTime();

  /**
   * @return true if this is a date property
   */
  boolean isDate();

  /**
   * @return true if this is a timestamp property
   */
  boolean isTimestamp();

  /**
   * @return true if this is a character property
   */
  boolean isCharacter();

  /**
   * @return true if this is a string property
   */
  boolean isString();

  /**
   * @return true if this is a integer property
   */
  boolean isInteger();

  /**
   * @return true if this is a double property
   */
  boolean isDouble();

  /**
   * @return true if this is a boolean property
   */
  boolean isBoolean();

  /**
   * @return true if this is a reference property
   */
  boolean isReference();

  /**
   * @param propertyID the property ID
   * @return true if this property is of the given type
   */
  boolean is(final String propertyID);

  /**
   * The property identifier, should be unique within an Entity.
   * By default this ID serves as column name for database properties.
   * @see #getPropertyID()
   * @return the ID of this property
   */

  String getPropertyID();

  /**
   * @return the data type of the value of this property
   */
  int getType();

  /**
   * @param type the type to check
   * @return true if the type of this property is the one given
   */
  boolean isType(final int type);

  /**
   * Sets the default value for this property, overrides the underlying column default value, if any
   * @param defaultValue the value to use as default
   * @return the property
   */
  Property setDefaultValue(final Object defaultValue);

  /**
   * @return the default value for this property, if any
   */
  Object getDefaultValue();

  /**
   * @return true if this property should be hidden in table views
   */
  boolean isHidden();

  /**
   * @param hidden specifies whether this property should not be visible to the user
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
   * Sets the maximum fraction digits to show for this property, only applicable to DOUBLE properties
   * This setting is overridden during subsquence calls to <code>setFormat</code>
   * @param maximumFractionDigits the maximum fraction digits
   * @return this Property instance
   */
  Property setMaximumFractionDigits(final int maximumFractionDigits);

  /**
   * @return the maximum number of fraction digits to show for this property value
   */
  int getMaximumFractionDigits();

  /**
   * Specifies whether to use number grouping when presenting this value.
   * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale.
   * Only applicable to numerical properties.
   * This setting is overridden during subsquence calls to <code>setFormat</code>
   * @param useGrouping if true then number grouping is used
   * @return this Property instance
   */
  Property setUseNumberFormatGrouping(final boolean useGrouping);

  /**
   * @return the preferred column width of this property when
   * presented in a table, 0 if none has been specified
   */
  int getPreferredColumnWidth();

  /**
   * @param preferredColumnWidth the preferred column width to be used when this property is shown in a table
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
   * properties that have parent properties (properties which comprise a fk property fx)
   * inherit the nullable state of the parent property.
   * @param nullable specifies whether or not this property accepts a null value
   * @return this Property instance
   */
  Property setNullable(final boolean nullable);

  /**
   * @return true if this property accepts a null value
   */
  boolean isNullable();

  /**
   * Sets the maximum length of this property value
   * @param maxLength the maximum length
   * @return this Property instance
   */
  Property setMaxLength(final int maxLength);

  /**
   * @return the maximum length of this property value, -1 is returned if the max length is undefined
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
   */
  Property setFormat(final Format format);

  /**
   * Specifies whether or not this attribute is read only
   * @return true if this attribute is read only
   */
  boolean isReadOnly();

  /**
   * @return true if this property has a parent property
   */
  boolean hasParentProperty();

  void setParentProperty(final ForeignKeyProperty foreignKeyProperty);

  ForeignKeyProperty getParentProperty();

  interface SearchableProperty extends Property {}

  interface ColumnProperty extends SearchableProperty, Column {

    /**
     * @param updatable specifies whether this property is updatable
     * @return this Property instance
     */
    ColumnProperty setUpdatable(boolean updatable);

    /**
     * Sets the select column index
     * @param selectIndex the index
     */
    void setSelectIndex(final int selectIndex);

    /**
     * @param columnHasDefaultValue specifies whether or not the underlying column has a default value
     * @return this Property instance
     */
    ColumnProperty setColumnHasDefaultValue(final boolean columnHasDefaultValue);

    ColumnProperty setSearchable(final boolean searchable);
  }

  /**
   * A property that is part of a entities primary key.
   * A primary key property is by default non-updatable.
   */
  interface PrimaryKeyProperty extends ColumnProperty {
    /**
     * @return this property's index in the primary key
     */
    int getIndex();

    /**
     * Sets the primary key index of this property
     * @param index the index
     * @return this PrimaryKeyProperty instance
     */
    PrimaryKeyProperty setIndex(final int index);
  }

  /**
   * A meta property that represents a reference to another entity, typically but not necessarily based on a foreign key.
   * These do not map directly to a underlying table column, but wrap the actual column properties involved in the relation.
   * e.g.: new Property.ForeignKeyProperty("reference_fk", new Property("reference_id")), where "reference_id" is the
   * actual name of the column involved in the reference, but "reference_fk" is simply a descriptive property ID
   */
  interface ForeignKeyProperty extends SearchableProperty {

    /**
     * @return the ID of the referenced entity
     */
    String getReferencedEntityID();

    /**
     * Returns a list containing the actual reference properties,
     * N.B. this list should not be modified.
     * @return the reference properties
     */
    List<ColumnProperty> getReferenceProperties();

    /**
     * @return true if this reference is based on more than on column
     */
    boolean isCompositeReference();

    int getFetchDepth();

    String getReferencedPropertyID(final Property referenceProperty);

    ForeignKeyProperty setFetchDepth(final int fetchDepth);


  }

  /**
   * Represents a child foreign key property that is already included as part of another reference foreign key property,
   * and should not handle updating the underlying property
   */
  //todo better explanation
  interface MirrorProperty extends ColumnProperty {

  }

  /**
   * A property representing a column that should get its value automatically from a column in a referenced table
   */
  interface DenormalizedProperty extends ColumnProperty {

    /**
     * @return the id of the foreign key property (entity) from which this property should retrieve its value
     */
    String getForeignKeyPropertyID();

    /**
     * @return the property from which this property gets its value
     */
    Property getDenormalizedProperty();
  }

  /**
   * A property based on a list of values, each with a displayable caption.
   */
  interface ValueListProperty extends ColumnProperty {

    boolean isValid(final Object value);

    /**
     * @return an unmodifiable view of the available values
     */
    List<Item<Object>> getValues();

    String getCaption(final Object value);
  }

  /**
   * A property that does not map to an underlying database column. The value of a transient property
   * is initialized to null when entities are loaded, which means transient properties always have
   * a original value of null.
   * The value of transient properties can be set and retrieved like normal properties.
   */
  interface TransientProperty extends Property {

  }

  /**
   * A property which value is derived from the values of one or more properties.
   * For the property to be updated when the parent properties are you must
   * link the properties together using the <code>addLinkedPropertyIDs()</code>method.
   * @see org.jminor.framework.domain.EntityRepository.Proxy#getDerivedValue(Entity, org.jminor.framework.domain.Property.DerivedProperty)
   */
  interface DerivedProperty extends TransientProperty {

    /**
     * @return the IDs of properties that trigger a change event for this property
     */
    Collection<String> getLinkedPropertyIDs();

    /**
     * Adds a property change link on the property identified by <code>linkedPropertyID</code>,
     * so that changes in that property trigger a change in this property
     * @param linkedPropertyIDs the IDs of the properties on which to link
     * @return this TransientProperty instance
     */
    DerivedProperty addLinkedPropertyIDs(final String... linkedPropertyIDs);
  }

  /**
   * A property that gets its value from a entity referenced by a foreign key, but is for
   * display only, and does not map to a database column
   */
  interface DenormalizedViewProperty extends TransientProperty {

    /**
     * @return the id of the foreign key property (entity) from which this property should retrieve its value
     */
    String getForeignKeyPropertyID();

    /**
     * @return the property from which this property gets its value
     */
    Property getDenormalizedProperty();
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
   * A boolean property, with special handling since different values
   * are used for representing boolean values in different systems
   */
  interface BooleanProperty extends ColumnProperty {

    /**
     * @return the data type of the underlying column
     */
    int getColumnType();

    /**
     * @param object the Object value to translate into a Boolean value
     * @return the Boolean value of <code>object</code>
     */
    Boolean toBoolean(final Object object);

    /**
     * @param value the Boolean value to translate into a sql string value
     * @return the sql string value of <code>value</code>
     */
    String toSQLString(final Boolean value);

    Object toSQLValue(final Boolean value);
  }

  /**
   * A BLOB property, based on two columns, the actual BLOB column and a column containing the name of the BLOB object.
   */
  interface BlobProperty extends ColumnProperty {

    String getBlobColumnName();
  }

  interface AuditProperty extends ColumnProperty {
    enum AuditAction {
      INSERT, UPDATE
    }

    AuditAction getAuditAction();
  }

  interface AuditTimeProperty extends AuditProperty {

  }

  interface AuditUserProperty extends AuditProperty {

  }
}
