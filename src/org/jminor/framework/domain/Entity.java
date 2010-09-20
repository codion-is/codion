/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.valuemap.ValueChangeMap;
import org.jminor.common.model.valuemap.ValueMap;
import org.jminor.common.model.valuemap.ValueMapValidator;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.RangeValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.common.model.IdSource;

import java.awt.Color;
import java.sql.Timestamp;
import java.text.Format;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * An ORM entity interface, providing access to the property values via the ValueMap interface.
 */
public interface Entity extends ValueChangeMap<String, Object>, Comparable<Entity> {

  /**
   * @return the entity ID
   */
  String getEntityID();

  /**
   * @return the primary key of this entity
   */
  Key getPrimaryKey();

  /**
   * @return the primary key of this entity in it's original state
   */
  Key getOriginalPrimaryKey();

  /**
   * Retrieves the property identified by propertyID from the entity repository
   * @param propertyID the ID of the property to retrieve
   * @return the property identified by propertyID
   */
  Property getProperty(final String propertyID);

  /**
   * @param property the property for which to retrieve the value
   * @return the value of the given property
   */
  Object getValue(final Property property);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a String
   * @throws ClassCastException if the value is not a String instance
   */
  String getStringValue(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is an Integer
   * @throws ClassCastException if the value is not a Integer instance
   */
  Integer getIntValue(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Character
   * @throws ClassCastException if the value is not a Character instance
   */
  Character getCharValue(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Double
   * @throws ClassCastException if the value is not a Double instance
   */
  Double getDoubleValue(final String propertyID);

  /**
   * @param propertyID the ID of the date property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Date
   * @throws ClassCastException if the value is not a Date instance
   */
  Date getDateValue(final String propertyID);

  /**
   * @param propertyID the ID of the date property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Timestamp
   * @throws ClassCastException if the value is not a Timestamp instance
   */
  Timestamp getTimestampValue(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Boolean
   * @throws ClassCastException if the value is not a Boolean instance
   */
  Boolean getBooleanValue(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the formatted value
   * @return the value of the property identified by <code>propertyID</code>, formatting it
   * with the format object associated with the property
   */
  String getFormattedValue(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the formatted value
   * @param format the format object
   * @return the value of the property identified by <code>propertyID</code>, formatted with <code>format</code>
   */
  String getFormattedValue(final String propertyID, final Format format);

  /**
   * @param property the property for which to retrieve the formatted value
   * @return the value of the given property formatted with the format object associated with the property
   */
  String getFormattedValue(final Property property);

  /**
   * @param property the property for which to retrieve the formatted value
   * @param format the format object
   * @return the value of the given property, formatted with <code>format</code>
   */
  String getFormattedValue(final Property property, final Format format);

  /**
   * This method returns a String representation of the value associated with the given property,
   * if the property has a format it is used.
   * @param property the property for which to retrieve the value
   * @return a String representation of the value of <code>property</code>
   * @see #getFormattedValue(Property, java.text.Format)
   */
  String getValueAsString(final Property property);

  /**
   * @param foreignKeyPropertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is an Entity
   * @throws RuntimeException if the property is not a foreign key property
   */
  Entity getForeignKeyValue(final String foreignKeyPropertyID);

  /**
   * Returns the primary key of the entity referenced by the given ForeignKeyProperty,
   * if the reference is null this method returns null.
   * @param foreignKeyProperty the foreign key property for which to retrieve the underlying EntityKey
   * @return the primary key of the underlying entity, null if no entity is referenced
   */
  Key getReferencedPrimaryKey(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * Returns true if the value of the given foreign key is null, in case of composite
   * foreign keys a single null value is enough.
   * @param foreignKeyProperty the foreign key property
   * @return true if the foreign key value is null
   */
  boolean isForeignKeyNull(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * Sets the value of the given property
   * @param property the property
   * @param value the value
   * @return the previous value
   */
  Object setValue(final Property property, final Object value);

  /**
   * @return true if the this entity instance has a null primary key
   * todo rename to isNew()?
   */
  boolean isNull();

  /**
   * @param property the property
   * @return true if the given property has a null value
   */
  boolean isValueNull(final Property property);

  /**
   * @param property the property
   * @return true if this entity contains a value for the given property
   */
  boolean containsValue(final Property property);

  /**
   * Initializes the given value assuming it has no previously set value.
   * This method does not propagate foreign key values nor set denormalized values.
   * This method should be used with care, if at all.
   * @param property the property for which to initialize the value
   * @param value the value
   */
  void initializeValue(final Property property, final Object value);

  /**
   * @param entityID the entityID
   * @return true if this entity is of the given type
   */
  boolean is(final String entityID);

  /**
   * @param entity the entity to compare to
   * @return true if all property values are equal
   */
  boolean propertyValuesEqual(final Entity entity);

  /**
   * Returns true if the entity referenced via the given foreign key property has been loaded
   * @param foreignKeyPropertyID the property id
   * @return true if the reference entity has been loaded
   */
  boolean isLoaded(final String foreignKeyPropertyID);

  /**
   * @param property the property for which to retrieve the background color
   * @return the background color to use when displaying this property in a table
   */
  Color getBackgroundColor(final Property property);

  /**
   * A class representing a primary key.
   */
  interface Key extends ValueMap<String, Object> {

    /**
     * @return the entity ID
     */
    String getEntityID();

    /**
     * @return a List containing the properties comprising this key
     */
    List<Property.PrimaryKeyProperty> getProperties();

    /**
     * @return the number of properties comprising this key
     */
    int getPropertyCount();

    /**
     * @return true if one of the primary key properties has a null value
     */
    boolean isNull();

    /**
     * @return true if this primary key is based on a single integer column
     */
    boolean isSingleIntegerKey();

    /**
     * @return true if this key is comprised of multiple properties.
     */
    boolean isCompositeKey();

    /**
     * @return the first key property, useful for single property keys
     */
    Property.PrimaryKeyProperty getFirstKeyProperty();

    /**
     * @return the first value contained in this key, useful for single property keys
     */
    Object getFirstKeyValue();
  }

  /**
   * Provides background colors for entities.
   */
  interface BackgroundColorProvider {

    /**
     * @param entity the entity
     * @param property the property
     * @return the background color to use for this entity and property
     */
    Color getBackgroundColor(final Entity entity, final Property property);
  }

  /**
   * Provides comparisons for entities.
   */
  interface Comparator {

    /**
     * Compares the given entities.
     * @param entity the first entity
     * @param entityToCompare the second entity
     * @return the compare result
     */
    int compare(final Entity entity, final Entity entityToCompare);
  }

  /**
   * Responsible for providing validation for entities.
   */
  interface Validator extends ValueMapValidator<String, Object> {

    /**
     * @return the ID of the entity this validator validates
     */
    String getEntityID();

    /**
     * Validates the values in the given entity
     * @param entity the entity to validate
     * @param action the action requiring validation
     * @throws org.jminor.common.model.valuemap.exception.ValidationException in case the validation fails
     * @see Property#setNullable(boolean)
     * @see org.jminor.framework.Configuration#PERFORM_NULL_VALIDATION
     */
    void validate(final Entity entity, final int action) throws ValidationException;

    /**
     * Validates the given property in the given entity
     * @param entity the entity to validate
     * @param propertyID the ID of the property to validate
     * @param action the action requiring validation
     * @throws org.jminor.common.model.valuemap.exception.ValidationException in case the validation fails
     * @see Property#setNullable(boolean)
     * @see org.jminor.framework.Configuration#PERFORM_NULL_VALIDATION
     */
    void validate(final Entity entity, final String propertyID, final int action) throws ValidationException;

    /**
     * Validates the given Entity objects.
     * @param entities the entities to validate
     * @param action describes the action requiring validation,
     * EntityEditor.INSERT, EntityEditor.UPDATE or EntityEditor.UNKNOWN
     * @throws org.jminor.common.model.valuemap.exception.ValidationException in case the validation fails
     */
    void validate(final Collection<Entity> entities, final int action) throws ValidationException;

    /**
     * Performs a null validation on the given property
     * @param entity the entity
     * @param property the property
     * @param action the action requiring validation
     * @throws org.jminor.common.model.valuemap.exception.NullValidationException in case the proerty value is null and the property is not nullable
     * @see Property#isNullable()
     */
    void performNullValidation(final Entity entity, final Property property, final int action) throws NullValidationException;

    /**
     * Performs a range validation on the given property
     * @param entity the entity
     * @param property the property
     * @throws org.jminor.common.model.valuemap.exception.RangeValidationException in case the value of the given property is outside the legal range
     * @see org.jminor.framework.domain.Property#setMax(double)
     * @see org.jminor.framework.domain.Property#setMin(double)
     */
    void performRangeValidation(final Entity entity, final Property property) throws RangeValidationException;
  }

  /**
   * Specifies a entity definition.
   */
  interface Definition {

    /**
     * @return the entity ID
     */
    String getEntityID();

    /**
     * @return the name of the underlying table, with schema prefix if applicable
     */
    String getTableName();

    /**
     * @return the ID of the domain this entity type belongs to
     */
    String getDomainID();

    /**
     * Sets the domain ID for this entity type
     * @param domainID the domain ID
     * @return this Entity.Definition instance
     */
    Definition setDomainID(final String domainID);

    /**
     * @param toString the to string provider
     * @return this Entity.Definition instance
     */
    Definition setToStringProvider(final ToString<String> toString);

    /**
     * @param colorProvider the background color provider
     * @return this Entity.Definition instance
     */
    Definition setBackgroundColorProvider(final BackgroundColorProvider colorProvider);

    /**
     * @param validator the validator for this entity type
     * @return this Entity.Definition instance
     */
    Definition setValidator(final Validator validator);

    /**
     * @return the validator for this enitity type
     */
    Validator getValidator();

    /**
     * @return the caption to use when presenting entities of this type
     */
    String getCaption();

    /**
     * Sets the caption for this entity type
     * @param caption the caption
     * @return this Entity.Definition instance
     */
    Definition setCaption(final String caption);

    /**
     * @return true if the underlying table is small enough for displaying the contents in a combo box
     */
    boolean isSmallDataset();

    /**
     * Specifies whether or not this entity should be regarded as based on a small dataset,
     * which primarily means that combo box models can be based on this entity.
     * This is false by default.
     * @param smallDataset true if the underlying table is small enough for displaying the contents in a combo box
     * @return this Entity.Definition instance
     */
    Definition setSmallDataset(final boolean smallDataset);

    /**
     * @return true if this entity type is read only
     */
    boolean isReadOnly();

    /**
     * Sets the read only value
     * @param readOnly true if this entity type should be read only
     * @return this Entity.Definition instance
     */
    Definition setReadOnly(final boolean readOnly);

    /**
     * @return the IdSource specified for this entity type
     */
    IdSource getIdSource();

    /**
     * Sets the id source for this entity type, which specifies the primary key
     * generation strategy to use.
     * @param idSource the idSource
     * @return this Entity.Definition instance
     */
    Definition setIdSource(final IdSource idSource);

    /**
     * @return the id value source
     */
    String getIdValueSource();

    /**
     * Sets the id value source for this entity type, such as sequence or table name,
     * depending on the underlying primary key generation strategy.
     * @param idValueSource the id value source
     * @return this Entity.Definition instance
     */
    Definition setIdValueSource(final String idValueSource);

    /**
     * @return the order by clause to use when querying entities of this type,
     * without the "order by" keywords
     */
    String getOrderByClause();

    /**
     * Sets the order by clause for this entity type, this clause should not
     * include the "order by" keywords.
     * @param orderByClause the order by clause
     * @return this Entity.Definition instance
     */
    Definition setOrderByClause(final String orderByClause);

    /**
     * @return the name of the table to use when selecting entities of this type
     */
    String getSelectTableName();

    /**
     * Sets the name of the table to use when selecting entities of this type,
     * when it differs from the one used to update/insert, such as a view.
     * @param selectTableName the name of the table
     * @return this Entity.Definition instance
     */
    Definition setSelectTableName(final String selectTableName);

    /**
     * @return the select query to use when selecting entities of this type
     */
    String getSelectQuery();

    /**
     * Sets the select query to use when selecting entities of this type,
     * use with care.
     * @param selectQuery the select query to use for this entity type
     * @return this Entity.Definition instance
     */
    Definition setSelectQuery(final String selectQuery);

    /**
     * @return the object responsible for providing toString values for this entity type
     */
    ToString<String> getStringProvider();

    /**
     * Sets the string provider, that is, the object responsible for providing toString values for this entity type
     * @param stringProvider the string provider
     * @return this Entity.Definition instance
     */
    Definition setStringProvider(final ToString<String> stringProvider);

    /**
     * Sets the comparator to use when comparing this entity type to other entities
     * @param comparator the comparator
     * @return this Entity.Definition instance
     */
    Definition setComparator(final Comparator comparator);

    /**
     * @return the comparator used when comparing this entity type to other entities
     */
    Comparator getComparator();

    /**
     * @return a list of property IDs identifying the properties to use when performing
     * a default lookup for this entity type
     */
    List<String> getSearchPropertyIDs();

    /**
     * Sets the IDs of the properties to use when performing a default lookup for this entity type
     * @param searchPropertyIDs the search property IDs
     * @return this Entity.Definition instance
     */
    Definition setSearchPropertyIDs(final String... searchPropertyIDs);

    /**
     * @return the properties for this entity type
     */
    Map<String, Property> getProperties();

    /**
     * Returns true if this entity contains properties which values are linked to the value of the given property
     * @param propertyID the ID of the property
     * @return true if any properties are linked to the given property
     */
    boolean hasLinkedProperties(final String propertyID);

    /**
     * Returns the IDs of the properties which values are linked to the value of the given property,
     * an empty collection if no such linked properties exist
     * @param propertyID the ID of the property
     * @return a collection conaining the IDs of any properties which are linked to the given property
     */
    Collection<String> getLinkedPropertyIDs(final String propertyID);

    /**
     * @return the primary key properties of this entity type
     */
    List<Property.PrimaryKeyProperty> getPrimaryKeyProperties();

    /**
     * Retrieves the column list to use when constructing a select query for this entity type
     * @return the query column list, i.e. "col1, col2, col3,..."
     */
    String getSelectColumnsString();

    /**
     * @return a list containing the visible properties for this entity type
     */
    List<Property> getVisibleProperties();

    /**
     * @return a list containing the column-based properties for this entity type
     */
    List<Property.ColumnProperty> getColumnProperties();

    /**
     * @return a list containing the non-column-based properties for this entity type
     */
    List<Property.TransientProperty> getTransientProperties();

    /**
     * @return a list containing the foreign key properties for this entity type
     */
    List<Property.ForeignKeyProperty> getForeignKeyProperties();

    /**
     * @return true if this entity type has any denormalized properties
     */
    boolean hasDenormalizedProperties();

    /**
     * @param foreignKeyPropertyID the ID of the foreign key property
     * @return true if this entity type has any denormalized properties associated with the give foreign key
     */
    boolean hasDenormalizedProperties(final String foreignKeyPropertyID);

    /**
     * Retrieves the denormalized properties which values originate from the entity referenced by the given foreign key property
     * @param foreignKeyPropertyID the foreign key property ID
     * @return a collection containing the denormalized properties which values originate from the entity
     * referenced by the given foreign key property
     */
    Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String foreignKeyPropertyID);

    /**
     * Compares the given entities.
     * @param entity the first entity
     * @param entityToCompare the second entity
     * @return the compare result
     */
    int compareTo(final Entity entity, final Entity entityToCompare);

    /**
     * @param entity the entity
     * @return a string representation of the given entity
     */
    String toString(final Entity entity);

    /**
     * @param entity the entity
     * @param property the property
     * @return the background color to use for this entity and property
     */
    Color getBackgroundColor(final Entity entity, final Property property);
  }
}
