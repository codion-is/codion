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
   * @return the property identifier of this property
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

  Property setDefaultValue(final Object defaultValue);

  /**
   * @return true if this property should be hidden in table views
   */
  boolean isHidden();

  Property setHidden(boolean hidden);

  /**
   * @return the maximum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  Double getMax();

  Property setMax(double max);

  /**
   * @return the minimum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  Double getMin();

  Property setMin(double min);

  Property setMaximumFractionDigits(int maximumFractionDigits);

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
   * @param preferredColumnWidth the preferred column width to be used when this property is shown in a table
   * @return this Property instance
   */
  Property setPreferredColumnWidth(final int preferredColumnWidth);

  Property setReadOnly(boolean readOnly);

  Property setNullable(boolean nullable);

  Property setMaxLength(int maxLength);

  /**
   * @return the preferred column width of this property when
   * presented in a table, 0 if none has been specified
   */
  int getPreferredColumnWidth();

  /**
   * @return the mnemonic to use when creating a label for this property
   */
  Character getMnemonic();

  /**
   * @return the Format object used to format the value of properties when being presented
   */
  Format getFormat();

  boolean hasParentProperty();

  void setParentProperty(final ForeignKeyProperty foreignKeyProperty);

  ForeignKeyProperty getParentProperty();

  interface SearchableProperty extends Property {}

  interface ColumnProperty extends SearchableProperty, Column {

    ColumnProperty setUpdatable(boolean updatable);

    void setSelectIndex(final int selectIndex);
  }

  /**
   * A property that is part of a entities primary key.
   * A primary key property is by default non-updatable.
   */
  interface PrimaryKeyProperty extends ColumnProperty {

    int getIndex();

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
     * @return an unmodifiable view of the values
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