/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.framework.Configuration;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.text.Format;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Represents a entity property, for example a database column.
 */
public class Property implements Serializable {

  private static final long serialVersionUID = 1;

  /**
   * The property identifier, should be unique within an Entity.
   * This ID serves as column name for database properties.
   * @see #getPropertyID
   */
  private final String propertyID;

  /**
   * The property type
   */
  private final Type propertyType;

  /**
   * The caption to use when this property is presented
   */
  private final String caption;

  /**
   * The name of the underlying column
   * Only applicable to properties that map to an underlying table column
   */
  private final String columnName;

  /**
   * A reference to a parent foreign key property, if one exists
   */
  private ForeignKeyProperty parentProperty;

  /**
   * A default value for this property in new Entity instances
   */
  private Object defaultValue;

  /**
   * The preferred column width when this property is presented in a table
   */
  private int preferredColumnWidth = -1;

  /**
   * True if this property should be hidden in table views
   */
  private boolean hidden = false;

  /**
   * True if this property is for selecting only, implicitly not updatable
   * and not used in insert statements
   */
  private boolean readOnly = false;

  /**
   * True if this property is updatable
   */
  private boolean updatable = true;

  /**
   * True if the value of this property is allowed to be null
   */
  private boolean nullable = true;

  /**
   * True if this property should be included when searching for entities
   */
  private boolean searchable = true;

  /**
   * True if the underlying column has a default value
   */
  private boolean columnHasDefaultValue = false;

  /**
   * The maximum length of the data
   */
  private int maxLength = -1;

  /**
   * The maximum number of fraction digits for this property.
   * Only applicable to DOUBLE properties
   */
  private int maximumFractionDigits = -1;

  /**
   * The maximum value for this property.
   * Only applicable to numerical properties
   */
  private Double max;

  /**
   * The minimum value for this property.
   * Only applicable to numerical properties
   */
  private Double min;

  /**
   * Specifies whether or not to use number format grouping in table views,
   * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale.
   * Only applicable to numerical properties
   */
  private boolean useNumberFormatGrouping = Configuration.getBooleanValue(Configuration.USE_NUMBER_FORMAT_GROUPING);

  /**
   * A string describing this property
   */
  private String description;

  /**
   * A mnemonic to use when creating a label for this property
   */
  private Character mnemonic;

  /**
   * Cached select column index
   */
  private int selectIndex = -1;

  /**
   * The Format used when presenting this Property value
   */
  private Format format;

  /**
   * Instantiates a new property of the type Type.INT
   * @param propertyID the property ID, this is used as the underlying column name
   */
  public Property(final String propertyID) {
    this(propertyID, Type.INT);
  }

  /**
   * @param propertyID the property ID, this is used as the underlying column name
   * @param propertyType the data type of this property
   */
  public Property(final String propertyID, final Type propertyType) {
    this(propertyID, propertyType, null);
  }

  /**
   * @param propertyID the property ID, this is used as the underlying column name
   * @param propertyType the data type of this property
   * @param caption the caption of this property, if this is null then this property is defined as hidden
   */
  public Property(final String propertyID, final Type propertyType, final String caption) {
    if (propertyID == null)
      throw new IllegalArgumentException("Property ID must be specified");
    if (propertyType == null)
      throw new IllegalArgumentException("Property type must be specified");
    setHidden(caption == null);
    this.propertyID = propertyID;
    this.propertyType = propertyType;
    this.caption = caption;
    this.columnName = propertyID;
  }

  /**
   * @return a String representation of this property
   */
  @Override
  public String toString() {
    return caption != null ? caption : propertyID;
  }

  /**
   * @param property the property
   * @return true if this property is of the given type
   */
  public boolean is(final Property property) {
    return is(property.propertyID);
  }

  /**
   * @return true if this is a numerical Property, that is, INT or DOUBLE
   */
  public boolean isNumerical() {
    return propertyType == Type.DOUBLE || propertyType == Type.INT;
  }

  /**
   * @param propertyID the property ID
   * @return true if this property is of the given type
   */
  public boolean is(final String propertyID) {
    return this.propertyID.equals(propertyID);
  }

  /**
   * @return the property identifier of this property
   */
  public String getPropertyID() {
    return this.propertyID;
  }

  /**
   * @return the data type of the value of this property
   */
  public Type getPropertyType() {
    return propertyType;
  }

  /**
   * @return the name of the underlying column
   */
  public String getColumnName() {
    return this.columnName;
  }

  /**
   * @param hidden specifies whether this property should not be visible to the user
   * @return this Property instance
   */
  public Property setHidden(final boolean hidden) {
    this.hidden = hidden;
    return this;
  }

  /**
   * @return true if this property should be hidden in table views
   */
  public boolean isHidden() {
    return hidden;
  }

  /**
   * @param updatable specifies whether this property is updatable
   * @return this Property instance
   */
  public Property setUpdatable(final boolean updatable) {
    this.updatable = updatable;
    return this;
  }

  /**
   * @return true if this property is updatable
   */
  public boolean isUpdatable() {
    return this.updatable;
  }

  /**
   * @param readOnly specifies whether this property should be included during insert/update operations
   * @return this Property instance
   */
  public Property setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
    return this;
  }

  /**
   * @return true if this property is for select only
   */
  public boolean isReadOnly() {
    return this.readOnly;
  }

  /**
   * @param nullable specifies whether or not this property accepts a null value
   * @return this Property instance
   */
  public Property setNullable(final boolean nullable) {
    this.nullable = nullable;
    return this;
  }

  /**
   * @return true if this property accepts a null value
   */
  public boolean isNullable() {
    return nullable;
  }

  /**
   * @param searchable specifies whether or not this property should be included when searching for entities
   * @return this Property instance
   */
  public Property setSearchable(final boolean searchable) {
    this.searchable = searchable;
    return this;
  }

  /**
   * @return true if this property shuld be included in search criteria
   */
  public boolean isSearchable() {
    return searchable;
  }

  /**
   * @return true if the underlying column has a default value
   */
  public boolean columnHasDefaultValue() {
    return columnHasDefaultValue;
  }

  /**
   * @param columnHasDefaultValue specifies whether or not the underlying column has a default value
   * @return this Property instance
   */
  public Property setColumnHasDefaultValue(final boolean columnHasDefaultValue) {
    this.columnHasDefaultValue = columnHasDefaultValue;
    return this;
  }

  /**
   * Sets the default value for this property, overrides the underlying column default value, if any
   * @param defaultValue the value to use as default
   * @return the property
   */
  public Property setDefaultValue(final Object defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * @return the default value for this property
   */
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  /**
   * Sets the maximum length of this property value
   * @param maxLength the maximum length
   * @return this Property instance
   */
  public Property setMaxLength(final int maxLength) {
    this.maxLength = maxLength;
    return this;
  }

  /**
   * @return the maximum length of this property value, -1 is returned if the max length is undefined
   */
  public int getMaxLength() {
    return maxLength;
  }

  /**
   * Sets the maximum fraction digits to show for this property, only applicable to DOUBLE properties
   * @param maximumFractionDigits the maximum fraction digits
   * @return this Property instance
   */
  public Property setMaximumFractionDigits(final int maximumFractionDigits) {
    this.maximumFractionDigits = maximumFractionDigits;
    return this;
  }

  /**
   * @return the maximum number of fraction digits to show for this property value
   */
  public int getMaximumFractionDigits() {
    return maximumFractionDigits;
  }

  /**
   * @return the maximum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  public Double getMax() {
    return max;
  }

  /**
   * Sets the maximum allowed value for this property, only applicable to numerical properties
   * @param max the maximum allowed value
   * @return this Property instance
   */
  public Property setMax(final double max) {
    this.max = max;
    return this;
  }

  /**
   * @return the minimum allowed value for this property, null if none is defined,
   * only applicable to numerical properties
   */
  public Double getMin() {
    return min;
  }

  /**
   * Sets the minimum allowed value for this property, only applicable to numerical properties
   * @param min the minimum allowed value
   * @return this Property instance
   */
  public Property setMin(final double min) {
    this.min = min;
    return this;
  }

  /**
   * Specifies whether to use number grouping when presenting this value.
   * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale.
   * Only applicable to numerical properties
   * @param useGrouping if true then number grouping is used
   * @return this Property instance
   */
  public Property setUseNumberFormatGrouping(final boolean useGrouping) {
    this.useNumberFormatGrouping = useGrouping;
    return this;
  }

  /**
   * @return whether or not number grouping is used when this property value is presented
   */
  public boolean useNumberFormatGrouping() {
    return useNumberFormatGrouping;
  }

  /**
   * @param preferredColumnWidth the preferred column width to be used when this property is shown in a table
   * @return this Property instance
   */
  public Property setPreferredColumnWidth(final int preferredColumnWidth) {
    this.preferredColumnWidth = preferredColumnWidth;
    return this;
  }

  /**
   * @return the preferred column width of this property when
   * presented in a table, null if none has been specified
   */
  public Integer getPreferredColumnWidth() {
    return preferredColumnWidth;
  }

  /**
   * @return a String describing this property
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description a String describing this property
   * @return this Property instance
   */
  public Property setDescription(final String description) {
    this.description = description;
    return this;
  }

  /**
   * @return the mnemonic to use when creating a label for this property
   */
  public Character getMnemonic() {
    return mnemonic;
  }

  /**
   * Sets the mnemonic to use when creating a label for this property
   * @param mnemonic the mnemonic character
   * @return this Property instance
   */
  public Property setMnemonic(final Character mnemonic) {
    this.mnemonic = mnemonic;
    return this;
  }

  /**
   * @return the Format object used to format the value of properties when being presented
   */
  public Format getFormat() {
    return format;
  }

  /**
   * Sets the Format to use when presenting property values
   * @param format the format to use
   * @return this Property instance
   */
  public Property setFormat(final Format format) {
    this.format = format;
    return this;
  }

  /**
   * @return true if this property has a parent property
   */
  public boolean hasParentProperty() {
    return this.parentProperty != null;
  }

  /**
   * @return true if this property is a denormalized one which should get
   * its value from a parent foreign key property
   */
  public boolean isDenormalized() {
    return false;
  }

  /**
   * @return the caption used when the value of this property is presented
   */
  public String getCaption() {
    if (caption == null && hasParentProperty())
      return parentProperty.getCaption();

    return caption;
  }

  /**
   * Sets the select column index
   * @param index the index
   */
  public void setSelectIndex(final int index) {
    this.selectIndex = index;
  }

  /**
   * @return the index of this property in a select query
   */
  public int getSelectIndex() {
    return selectIndex;
  }

  /**
   * @return true if this property maps to a database column
   */
  public boolean isDatabaseProperty() {
    return true;
  }

  /**
   * @param object the object to compare with
   * @return true if object is a Property instance and has a
   * property identifier equal to that of this property
   */
  @Override
  public boolean equals(final Object object) {
    return this == object || object instanceof Property && this.propertyID.equals(((Property) object).propertyID);
  }

  /**
   * Sets the parent property
   * @param parentProperty the property to set as parent property
   */
  private void setParentProperty(final ForeignKeyProperty parentProperty) {
    this.parentProperty = parentProperty;
  }

  /**
   * A property that is part of a entities primary key.
   * A primary key property is by default non-updatable.
   */
  public static class PrimaryKeyProperty extends Property {

    /**
     * This property's index in the primary key
     */
    private int index = 0;

    public PrimaryKeyProperty(final String propertyID) {
      this(propertyID, Type.INT);
    }

    public PrimaryKeyProperty(final String propertyID, final Type propertyType) {
      this(propertyID, propertyType, null);
    }

    public PrimaryKeyProperty(final String propertyID, final Type propertyType, final String caption) {
      super(propertyID, propertyType, caption);
      setUpdatable(false);
    }

    public int getIndex() {
      return index;
    }

    public PrimaryKeyProperty setIndex(final int primaryKeyIndex) {
      if (primaryKeyIndex < 0)
        throw new IllegalArgumentException("Primary key index must be at least 0");
      this.index = primaryKeyIndex;
      return this;
    }
  }

  /**
   * A meta property that represents a reference to another entity, typically but not necessarily based on a foreign key.
   * These do not map directly to a underlying table column, but wrap the actual column properties involved in the relation.
   * e.g.: new Property.ForeignKeyProperty("reference_fk", new Property("reference_id")), where "reference_id" is the
   * actual name of the column involved in the reference, but "reference_fk" is simply a descriptive property ID
   */
  public static class ForeignKeyProperty extends Property {

    private final String referencedEntityID;

    private final List<Property> referenceProperties;

    private int fetchDepth = 0;

    /**
     * @param propertyID the property ID, since EntityProperties are meta properties, the property ID should not
     * be a underlying table column, it must only be unique for this entity
     * @param caption the property caption
     * @param referencedEntityID the ID of the referenced entity type
     * @param referenceProperties the actual column properties involved in the reference
     */
    public ForeignKeyProperty(final String propertyID, final String caption, final String referencedEntityID,
                              final Property... referenceProperties) {
      super(propertyID, Type.ENTITY, caption);
      for (final Property referenceProperty : referenceProperties)
        if (referenceProperty.propertyID.equals(propertyID))
          throw new IllegalArgumentException(referencedEntityID + ", reference property does not have a unique name: " + propertyID);
      if (referencedEntityID == null)
        throw new IllegalArgumentException("referencedEntityID is null: " + propertyID);

      for (final Property referenceProperty : referenceProperties)
        referenceProperty.setParentProperty(this);
      this.referencedEntityID = referencedEntityID;
      this.referenceProperties = Collections.unmodifiableList(Arrays.asList(referenceProperties));
    }

    /**
     * @return the ID of the referenced entity
     */
    public String getReferencedEntityID() {
      return referencedEntityID;
    }

    /**
     * Returns a list containing the actual reference properties,
     * N.B. this list should not be modified.
     * @return the reference properties
     */
    public List<Property> getReferenceProperties() {
      return referenceProperties;
    }

    /**
     * @return true if this reference is based on more than on column
     */
    public boolean isMultipleColumnReference() {
      return this.referenceProperties.size() > 1;
    }

    @Override
    public final String getColumnName() {
      throw new RuntimeException("Foreign key properties do not have column names");
    }

    public int getFetchDepth() {
      return fetchDepth;
    }

    public ForeignKeyProperty setFetchDepth(final int fetchDepth) {
      this.fetchDepth = fetchDepth;
      return this;
    }
  }

  /**
   * Represents a child foreign key property that is already included as part of another reference foreign key property,
   * and should not handle updating the underlying property
   */
  //todo better explanation
  public static class MirrorProperty extends Property {

    public MirrorProperty(final String propertyID) {
      super(propertyID);
    }
  }

  /**
   * A property representing a column that should get its value automatically from a column in a referenced table
   */
  public static class DenormalizedProperty extends Property {

    private final String foreignKeyPropertyID;

    public final Property denormalizedProperty;

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param foreignKeyPropertyID the ID of the foreign key property which references the entity which owns
     * the denormalized property
     * @param denormalizedProperty the property from which this property should get its value
     */
    public DenormalizedProperty(final String propertyID, final String foreignKeyPropertyID,
                                final Property denormalizedProperty) {
      this(propertyID, foreignKeyPropertyID, denormalizedProperty, null);
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param foreignKeyPropertyID the ID of the foreign key property which references the entity which owns
     * the denormalized property
     * @param denormalizedProperty the property from which this property should get its value
     * @param caption the caption if this property
     */
    public DenormalizedProperty(final String propertyID, final String foreignKeyPropertyID,
                                final Property denormalizedProperty, final String caption) {
      super(propertyID, denormalizedProperty.propertyType, caption);
      this.foreignKeyPropertyID = foreignKeyPropertyID;
      this.denormalizedProperty = denormalizedProperty;
    }

    /**
     * @return the id of the foreign key property (entity) from which this property should retrieve its value
     */
    public String getForeignKeyPropertyID() {
      return foreignKeyPropertyID;
    }

    /**
     * @return the property from which this property gets its value
     */
    public Property getDenormalizedProperty() {
      return denormalizedProperty;
    }

    @Override
    public boolean isDenormalized() {
      return true;
    }
  }

  /**
   * A property that does not map to an underlying database column. The value of a transient property
   * is initialized to null when entities are loaded.
   * The Entity.Proxy class can be used to provide the values of transient properties, by overriding it's getValue() method.
   * @see Entity#setDefaultProxy(org.jminor.framework.domain.Entity.Proxy)
   * @see Entity#setProxy(String, org.jminor.framework.domain.Entity.Proxy)
   * @see org.jminor.framework.domain.Entity.Proxy#getTransientValue(Entity, org.jminor.framework.domain.Property.TransientProperty)
   */
  public static class TransientProperty extends Property {

    private Collection<String> linkedPropertyIDs;

    /**
     * @param propertyID the property ID, since TransientProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the data type of this property
     */
    public TransientProperty(final String propertyID, final Type type) {
      this(propertyID, type, null);
    }

    /**
     * @param propertyID the property ID, since TransientProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the data type of this property
     * @param caption the caption of this property
     */
    public TransientProperty(final String propertyID, final Type type, final String caption) {
      super(propertyID, type, caption);
      super.setUpdatable(false);
      super.setSearchable(false);
    }

    @Override
    public final Property setUpdatable(final boolean updatable) {
      throw new IllegalArgumentException("TransientProperty can not be updatable");
    }

    @Override
    public final Property setSearchable(final boolean searchable) {
      throw new IllegalArgumentException("TransientProperty can not be searchable");
    }

    /**
     * @return true if this property maps to a database column
     */
    @Override
    public final boolean isDatabaseProperty() {
      return false;
    }

    @Override
    public final String getColumnName() {
      throw new RuntimeException("Transient properties do not have column names");
    }

    /**
     * @return the IDs of properties that trigger a change event for this property
     */
    public Collection<String> getLinkedPropertyIDs() {
      return linkedPropertyIDs;
    }

    /**
     * Adds a property change link on the property identified by <code>linkedPropertyID</code>,
     * so that changes in that property trigger a change in this property
     * @param linkedPropertyIDs the IDs of the properties on which to link
     * @return this TransientProperty instance
     */
    public TransientProperty addLinkedPropertyIDs(final String... linkedPropertyIDs) {
      if (this.linkedPropertyIDs == null)
        this.linkedPropertyIDs = new HashSet<String>();
      this.linkedPropertyIDs.addAll(Arrays.asList(linkedPropertyIDs));
      return this;
    }
  }

  /**
   * A property that gets its value from a entity referenced by a foreign key, but is for
   * display only, and does not map to a database column
   */
  public static class DenormalizedViewProperty extends TransientProperty {

    private final String foreignKeyPropertyID;

    private final Property denormalizedProperty;

    /**
     * @param propertyID the ID of the property, this should not be a column name since this property does not
     * map to a table column
     * @param foreignKeyPropertyID the ID of the foreign key property from which entity value this property gets its value
     * @param property the property from which this property gets its value
     */
    public DenormalizedViewProperty(final String propertyID, final String foreignKeyPropertyID, final Property property) {
      this(propertyID, foreignKeyPropertyID, property, null);
    }

    /**
     * @param propertyID the ID of the property, this should not be a column name since this property does not
     * map to a table column
     * @param foreignKeyPropertyID the ID of the foreign key property from which entity value this property gets its value
     * @param property the property from which this property gets its value
     * @param caption the caption of this property
     */
    public DenormalizedViewProperty(final String propertyID, final String foreignKeyPropertyID, final Property property,
                                    final String caption) {
      super(propertyID, property.propertyType, caption);
      this.foreignKeyPropertyID = foreignKeyPropertyID;
      this.denormalizedProperty = property;
    }


    /**
     * @return the id of the foreign key property (entity) from which this property should retrieve its value
     */
    public String getForeignKeyPropertyID() {
      return foreignKeyPropertyID;
    }

    /**
     * @return the property from which this property gets its value
     */
    public Property getDenormalizedProperty() {
      return denormalizedProperty;
    }

    @Override
    public boolean isDenormalized() {
      return true;
    }
  }

  /**
   * A property based on a subquery, returning a single value
   */
  public static class SubqueryProperty extends Property {

    private final String subquery;

    /**
     * @param propertyID the property ID, since SubqueryProperties do not map to underlying table columns,
     * the property ID should not be column name, only be unique for this entity
     * @param type the data type of this property
     * @param caption the caption of this property
     * @param subquery the sql query
     */
    public SubqueryProperty(final String propertyID, final Type type, final String caption, final String subquery) {
      super(propertyID, type, caption);
      super.setReadOnly(true);
      super.setUpdatable(false);
      this.subquery = subquery;
    }

    @Override
    public Property setUpdatable(final boolean updatable) {
      throw new IllegalArgumentException("SubqueryProperty can not be updatable");
    }

    @Override
    public Property setReadOnly(final boolean selectOnly) {
      throw new IllegalArgumentException("SubqueryProperty can only be select only");
    }

    /**
     * @return the subquery string
     */
    public String getSubQuery() {
      return subquery;
    }

    @Override
    public final String getColumnName() {
      throw new RuntimeException("Subquery properties do not have column names");
    }
  }

  /**
   * A boolean property, with special handling since different values
   * are used for representing boolean values in different systems
   */
  public static class BooleanProperty extends Property {

    private final Type columnType;
    /**
     * the Object value representing true
     */
    private final Object trueValue;
    /**
     * the Object value representing false
     */
    private final Object falseValue;
    /**
     * the Object value representing null
     */
    private final Object nullValue;
    /**
     * for quick comparison of 'true' values
     */
    private final int trueValueHash;
    /**
     * for quick comparison of 'false' values
     */
    private final int falseValueHash;

    /**
     * Instantiates a BooleanProperty based on the INT data type
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param caption the caption of this property
     */
    public BooleanProperty(final String propertyID, final String caption) {
      this(propertyID, Type.INT, caption);
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param columnType the data type of the underlying column
     * @param caption the caption of this property
     */
    public BooleanProperty(final String propertyID, final Type columnType, final String caption) {
      this(propertyID, columnType, caption, Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_TRUE),
              Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_FALSE));
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param columnType the data type of the underlying column
     * @param caption the caption of this property
     * @param trueValue the Object value representing 'true'
     * @param falseValue the Object value representing 'false'
     */
    public BooleanProperty(final String propertyID, final Type columnType, final String caption,
                           final Object trueValue, final Object falseValue) {
      this(propertyID, columnType, caption, trueValue, falseValue,
              Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_NULL));
    }

    /**
     * @param propertyID the property ID, in case of database properties this should be the underlying column name
     * @param columnType the data type of the underlying column
     * @param caption the caption of this property
     * @param trueValue the Object value representing 'true'
     * @param falseValue the Object value representing 'false'
     * @param nullValue the Object value representing 'null'
     */
    public BooleanProperty(final String propertyID, final Type columnType, final String caption,
                           final Object trueValue, final Object falseValue, final Object nullValue) {
      super(propertyID, Type.BOOLEAN, caption);
      this.columnType = columnType;
      this.nullValue = nullValue;
      this.trueValue = trueValue;
      this.falseValue = falseValue;
      this.trueValueHash = trueValue.hashCode();
      this.falseValueHash = falseValue.hashCode();
    }

    /**
     * @return the data type of the underlying column
     */
    public Type getColumnType() {
      return columnType;
    }

    /**
     * @param object the Object value to translate into a Boolean value
     * @return the Boolean value of <code>object</code>
     */
    public Boolean toBoolean(final Object object) {
      final int hashCode = object == null ? 0 : object.hashCode();
      if (hashCode == trueValueHash)
        return true;
      else if (hashCode == falseValueHash)
        return false;

      return null;
    }

    /**
     * @param value the Boolean value to translate into a sql string value
     * @return the sql string value of <code>value</code>
     */
    public String toSQLString(final Boolean value) {
      final Object result = value == null ? nullValue : (value ? trueValue : falseValue);
      if (columnType == Type.STRING)
        return "'" + result + "'";
      else
        return result == null ? "null" : result.toString();
    }

    public Object toSQLValue(final Boolean value) {
      return value == null ? nullValue : (value ? trueValue : falseValue);
    }
  }

  /**
   * A BLOB property, based on two columns, the actual BLOB column and a column containing the name of the BLOB object.
   */
  public static class BlobProperty extends Property {

    private final String blobColumnName;

    public BlobProperty(final String propertyID, final String blobColumnName, final String caption) {
      super(propertyID, Type.STRING, caption);
      super.setHidden(true);
      this.blobColumnName = blobColumnName;
    }

    public String getBlobColumnName() {
      return blobColumnName;
    }
  }

  /**
   * Used when property change events are fired
   */
  public static class Event extends ActionEvent {

    /**
     * The ID of the entity owning the property
     */
    private final String entityID;

    /**
     * The property
     */
    private final Property property;

    /**
     * The new property value
     */
    private final Object newValue;

    /**
     * The old property value
     */
    private final Object oldValue;

    /**
     * True if this change event is coming from the model, false if it is coming from the UI
     */
    private final boolean isModelChange;

    /**
     * True if this property change indicates an initialization, that is, the property did not
     * have a value before this value change
     */
    private final boolean initialization;

    /**
     * Instantiates a new PropertyEvent
     * @param source the source of the property value change
     * @param entityID the ID of the entity which owns the property
     * @param property the property
     * @param newValue the new value
     * @param oldValue the old value
     * @param isModelChange true if the value change originates from the model, false if it originates in the UI
     * @param initialization true if the property value was being initialized
     */
    public Event(final Object source, final String entityID, final Property property, final Object newValue,
                 final Object oldValue, final boolean isModelChange, final boolean initialization) {
      super(source, 0, property.getPropertyID());
      this.entityID = entityID;
      this.property = property;
      this.newValue = newValue;
      this.oldValue = oldValue;
      this.isModelChange = isModelChange;
      this.initialization = initialization;
    }

    /**
     * @return the ID of the entity owning the property
     */
    public String getEntityID() {
      return entityID;
    }

    /**
     * @return the property which value just changed
     */
    public Property getProperty() {
      return property;
    }

    /**
     * @return the property's old value
     */
    public Object getOldValue() {
      return oldValue;
    }

    /**
     * @return the property's new value
     */
    public Object getNewValue() {
      return newValue;
    }

    /**
     * @return true if this property change is coming from the model,
     * false if it is coming from the UI
     */
    public boolean isModelChange() {
      return isModelChange;
    }

    /**
     * @return true if this property change is coming from the UI,
     * false if it is coming from the model
     */
    public boolean isUIChange() {
      return !isModelChange;
    }

    /**
     * @return true if this property did not have a value prior to this value change
     */
    public boolean isInitialization() {
      return initialization;
    }
  }

  /**
   * Used when listening to Property.Events
   */
  public abstract static class Listener implements ActionListener {

    /** {@inheritDoc} */
    public final void actionPerformed(final ActionEvent event) {
      if (!(event instanceof Event))
        throw new IllegalArgumentException("Property.Listener can only be used with Property.Event, " + event);

      propertyChanged((Event) event);
    }

    protected abstract void propertyChanged(final Event event);
  }
}
