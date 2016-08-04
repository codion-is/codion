/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.valuemap.ValueMap;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.RangeValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Format;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Represents a row in a database table, providing access to the column values via the {@link ValueMap} interface.
 */
public interface Entity extends ValueMap<Property, Object>, Comparable<Entity> {

  /**
   * @return the entity ID
   */
  String getEntityID();

  /**
   * @return the primary key of this entity
   */
  Key getKey();

  /**
   * @return the primary key of this entity in its original state
   */
  Key getOriginalKey();

  /**
   * Retrieves the property identified by propertyID from the entity repository
   * @param propertyID the ID of the property to retrieve
   * @return the property identified by propertyID
   * @throws IllegalArgumentException in case the property does not exist in this entity
   */
  Property getProperty(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the given property
   */
  Object get(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the original value
   * @return the original value of the given property
   */
  Object getOriginal(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyID},
   * assuming it is a String
   * @throws ClassCastException if the value is not a String instance
   */
  String getString(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyID},
   * assuming it is an Integer
   * @throws ClassCastException if the value is not a Integer instance
   */
  Integer getInteger(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyID},
   * assuming it is a Character
   * @throws ClassCastException if the value is not a Character instance
   */
  Character getCharacter(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyID},
   * assuming it is a Double. Rounds the value before returning it in case
   * maximumFractionDigits have been specified.
   * @throws ClassCastException if the value is not a Double instance
   * @see Property#getMaximumFractionDigits()
   */
  Double getDouble(final String propertyID);

  /**
   * @param propertyID the ID of the date property for which to retrieve the value
   * @return the value of the property identified by {@code propertyID},
   * assuming it is a Date
   * @throws ClassCastException if the value is not a Date instance
   */
  Date getDate(final String propertyID);

  /**
   * @param propertyID the ID of the date property for which to retrieve the value
   * @return the value of the property identified by {@code propertyID},
   * assuming it is a Timestamp
   * @throws ClassCastException if the value is not a Timestamp instance
   */
  Timestamp getTimestamp(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyID},
   * assuming it is a Boolean
   * @throws ClassCastException if the value is not a Boolean instance
   */
  Boolean getBoolean(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the formatted value
   * @param format the format object
   * @return the value of the property identified by {@code propertyID}, formatted with {@code format}
   */
  String getFormatted(final String propertyID, final Format format);

  /**
   * @param property the property for which to retrieve the formatted value
   * @param format the format object
   * @return the value of the given property, formatted with {@code format}
   */
  String getFormatted(final Property property, final Format format);

  /**
   * This method returns a String representation of the value associated with the given property,
   * if the property has a format it is used.
   * @param propertyID the ID of the property for which to retrieve the value
   * @return a String representation of the value of {@code property}
   * @see #getFormatted(Property, java.text.Format)
   */
  String getAsString(final String propertyID);

  /**
   * Returns the Entity instance referenced by the given foreign key property.
   * If the underlying reference property contains a value, that is,
   * a foreign key value exists but the actual referenced entity has not
   * been loaded, an "empty" entity is returned, containing only the primary
   * key value. Null is returned only if the actual reference property is null.
   * @param foreignKeyPropertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyID},
   * assuming it is an Entity
   * @throws IllegalArgumentException if the property is not a foreign key property
   * @see #isLoaded(String)
   */
  Entity getForeignKey(final String foreignKeyPropertyID);

  /**
   * Returns the Entity instance referenced by the given foreign key property.
   * If the underlying reference property contains a value, that is,
   * a foreign key value exists but the actual referenced entity has not
   * been loaded, an "empty" entity is returned, containing only the primary
   * key value. Null is returned only if the actual reference property is null.
   * @param foreignKeyProperty the foreign key property for which to retrieve the value
   * @return the value of the foreign key property
   * @see #isLoaded(String)
   */
  Entity getForeignKey(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * Returns the primary key of the entity referenced by the given {@link Property.ForeignKeyProperty},
   * if the reference is null this method returns null.
   * @param foreignKeyProperty the foreign key property for which to retrieve the underlying {@link Entity.Key}
   * @return the primary key of the underlying entity, null if no entity is referenced
   */
  Key getReferencedKey(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * Returns true if the value of the given foreign key is null, in case of composite
   * foreign keys a single null value is enough.
   * @param foreignKeyProperty the foreign key property
   * @return true if the foreign key value is null
   */
  boolean isForeignKeyNull(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * Sets the value of the given property
   * @param propertyID the ID of the property
   * @param value the value
   * @return the previous value
   * @throws IllegalArgumentException in case the value type does not fit the property
   */
  Object put(final String propertyID, final Object value);

  /**
   * Sets the value of the given property
   * @param property the property
   * @param value the value
   * @param validateType if true then the type of the value is validated
   * @return the previous value
   * @throws IllegalArgumentException in case type validation is enabled, and the value type does not fit the property
   */
  Object put(final Property property, final Object value, final boolean validateType);

  /**
   * @return true if the this entity instance has a null primary key
   */
  boolean isKeyNull();

  /**
   * @param propertyID the propertyID
   * @return true if the value associated with the given property has been modified
   */
  boolean isModified(final String propertyID);

  /**
   * Clears the primary key values from this entity,
   * current as well as original values if any
   */
  void clearKeyValues();

  /**
   * @param entityID the entityID
   * @return true if this entity is of the given type
   */
  boolean is(final String entityID);

  /**
   * @param entity the entity to compare to
   * @return true if all property values are equal
   */
  boolean valuesEqual(final Entity entity);

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
  Object getBackgroundColor(final Property property);

  /**
   * Reverts the value associated with the given property to its original value.
   * If the value has not been modified then calling this method has no effect.
   * @param propertyID the ID of the property for which to revert the value
   */
  void revert(final String propertyID);

  /**
   * Saves the value associated with the given key, that is, removes the original value.
   * If no original value exists calling this method has no effect.
   * @param propertyID the ID of the property for which to save the value
   */
  void save(final String propertyID);

  /**
   * Returns true if a null value is mapped to the given property.
   * @param propertyID the ID of the property
   * @return true if the value mapped to the given property is null
   */
  boolean isValueNull(final String propertyID);

  /**
   * Returns true if this Entity contains a value for the given property, that value can be null.
   * @param propertyID the propertyID
   * @return true if a value is mapped to this property
   */
  boolean containsKey(final String propertyID);

  /**
   * Removes the given property and value from this Entity along with the original value if any.
   * If no value is mapped to the given property, this method has no effect.
   * @param propertyID the ID of the property to remove
   */
  void remove(final String propertyID);

  /**
   * A class representing a primary key.
   */
  interface Key extends ValueMap<Property.ColumnProperty, Object> {

    /**
     * @return the entity ID
     */
    String getEntityID();

    /**
     * @return a List containing the properties comprising this key
     */
    List<Property.ColumnProperty> getProperties();

    /**
     * @return the number of properties comprising this key
     */
    int getPropertyCount();

    /**
     * @return true if all of the primary key properties have a null value
     */
    boolean isNull();

    /**
     * Returns true if a null value is mapped to the given property.
     * @param propertyID the propertyID
     * @return true if the value mapped to the given property is null
     */
    boolean isValueNull(final String propertyID);

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
    Property.ColumnProperty getFirstProperty();

    /**
     * @return the first value contained in this key, useful for single property keys
     */
    Object getFirstValue();

    /**
     * @param propertyID the propertyID
     * @param value the value to associate with the property
     * @return the previous value
     */
    Object put(final String propertyID, final Object value);

    /**
     * @param propertyID the propertyID
     * @return the value associated with the given property
     */
    Object get(final String propertyID);
  }

  /**
   * Generates primary key values for entities on insert.
   * PrimaryKeyGenerators fall into two categories, one in which the primary key value is
   * fetched or generated before the record is inserted and one where the underlying database
   * automatically sets the primary key value on insert, f.ex. with a table trigger or identity columns.
   * Implementations should implement either {@code beforeInsert()} or {@code afterInsert()}
   * and leave the other one empty. {@code isAutoIncrement()} returns true if the database
   * generates primary key values automatically, this implies that {@code afterInsert()}
   * should be used, fetching the generated primary key value and updating the entity instance accordingly.
   */
  interface KeyGenerator {

    /**
     * The possible key generation strategy types
     */
    enum Type {
      /**
       * The primary key is not generated but set manually before insert
       */
      NONE(true, false),
      /**
       * The primary key value is fetched from a sequence before insert
       */
      SEQUENCE(false, false),
      /**
       * The primary key value is retreived via query before insert
       */
      QUERY(false, false),
      /**
       * The primary key value is generated by incrementing the max value of a column
       */
      INCREMENT(false, false),
      /**
       * The primary key value is automatically created by the underlying database
       */
      AUTOMATIC(false, true);

      private final boolean manual;
      private final boolean autoIncrement;

      Type(final boolean manual, final boolean autoIncrement) {
        this.manual = manual;
        this.autoIncrement = autoIncrement;
      }

      /**
       * @return true if the underlying database handles the key generation
       */
      public boolean isAutoIncrement() {
        return autoIncrement;
      }

      /**
       * @return true if the key value needs to be set manually before insert
       */
      public boolean isManual() {
        return manual;
      }
    }

    /**
     * Prepares the given entity for insert, that is, generates and fetches any required primary key values
     * and populates the entitys primary key
     * @param entity the entity to prepare
     * @param primaryKeyProperty the primary key property for which the value is being generated
     * @param connection the connection to use
     * @throws SQLException in case of an exception
     */
    void beforeInsert(final Entity entity, final Property.ColumnProperty primaryKeyProperty,
                      final DatabaseConnection connection) throws SQLException;

    /**
     * Prepares the given entity after insert, that is, fetches automatically generated primary
     * key values and populates the entitys primary key
     * @param entity the entity to prepare
     * @param primaryKeyProperty the primary key property for which the value is being generated
     * @param connection the connection to use
     * @throws SQLException in case of an exception
     */
    void afterInsert(final Entity entity, final Property.ColumnProperty primaryKeyProperty,
                     final DatabaseConnection connection) throws SQLException;

    /**
     * @return the key generator type
     */
    Type getType();
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
    Object getBackgroundColor(final Entity entity, final Property property);
  }

  /**
   * Responsible for providing validation for entities.
   */
  interface Validator extends ValueMap.Validator<Property, Entity> {

    /**
     * @return the ID of the entity this validator validates
     */
    String getEntityID();

    /**
     * Validates the given Entity objects.
     * @param entities the entities to validate
     * @throws ValidationException in case the validation fails
     */
    void validate(final Collection<Entity> entities) throws ValidationException;

    /**
     * Performs a null validation on the given property
     * @param entity the entity
     * @param property the property
     * @throws NullValidationException in case the property value is null and the property is not nullable
     * @see Property#isNullable()
     */
    void performNullValidation(final Entity entity, final Property property) throws NullValidationException;

    /**
     * Performs a range validation on the given property
     * @param entity the entity
     * @param property the property
     * @throws RangeValidationException in case the value of the given property is outside the legal range
     * @see org.jminor.framework.domain.Property#setMax(double)
     * @see org.jminor.framework.domain.Property#setMin(double)
     */
    void performRangeValidation(final Entity entity, final Property property) throws RangeValidationException;
  }

  /**
   * Describes an object responsible for providing String representations of entity instances
   */
  interface ToString {
    /**
     * Returns a string representation of the given entity
     * @param entity the entity
     * @return a string representation of the entity
     */
    String toString(final Entity entity);
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
     * Sets the underlying table name for this entity type
     * @param tableName the table name
     * @return this Entity.Definition instance
     */
    Definition setTableName(final String tableName);

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
     * @param colorProvider the background color provider
     * @return this {@link Entity.Definition} instance
     */
    Definition setBackgroundColorProvider(final BackgroundColorProvider colorProvider);

    /**
     * @param validator the validator for this entity type
     * @return this {@link Entity.Definition} instance
     */
    Definition setValidator(final Validator validator);

    /**
     * @return the validator for this entity type
     */
    Validator getValidator();

    /**
     * @return the ResultPacker responsible for packing this entity type
     */
    ResultPacker<Entity> getResultPacker();

    /**
     * @return the caption to use when presenting entities of this type
     */
    String getCaption();

    /**
     * Sets the caption for this entity type
     * @param caption the caption
     * @return this {@link Entity.Definition} instance
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
     * @return this {@link Entity.Definition} instance
     */
    Definition setSmallDataset(final boolean smallDataset);

    /**
     * @return true if the data in the underlying table can be regarded as static
     */
    boolean isStaticData();

    /**
     * Specifies whether or not this entity should be regarded as based on a static dataset, that is,
     * one that changes only infrequently.
     * This is false by default.
     * @param staticData true if the underlying table data is static
     * @return this {@link Entity.Definition} instance
     */
    Definition setStaticData(final boolean staticData);

    /**
     * @return true if this entity type is read only
     */
    boolean isReadOnly();

    /**
     * Sets the read only value, if true then it should not be possible to
     * insert, update or delete entities of this type
     * @param readOnly true if this entity type should be read only
     * @return this {@link Entity.Definition} instance
     */
    Definition setReadOnly(final boolean readOnly);

    /**
     * @return the object responsible for generating primary key values for entities of this type
     */
    KeyGenerator getKeyGenerator();

    /**
     * Sets the primary key generator
     * @param keyGenerator the primary key generator
     * @return this {@link Entity.Definition} instance
     */
    Definition setKeyGenerator(final KeyGenerator keyGenerator);

    /**
     * @return the type of key generator used for generating primary key values for entities of this type
     */
    KeyGenerator.Type getKeyGeneratorType();

    /**
     * Sets the type of primary key generator
     * @param keyGeneratorType the type of the primary key generator
     * @return this {@link Entity.Definition} instance
     */
    Definition setKeyGeneratorType(final KeyGenerator.Type keyGeneratorType);

    /**
     * @return the order by clause to use when querying entities of this type,
     * without the "order by" keywords
     */
    String getOrderByClause();

    /**
     * Sets the order by clause for this entity type, this clause should not
     * include the "order by" keywords.
     * @param orderByClause the order by clause
     * @return this {@link Entity.Definition} instance
     */
    Definition setOrderByClause(final String orderByClause);

    /**
     * @return the group by clause to use when querying entities of this type,
     * without the "group by" keywords
     */
    String getGroupByClause();

    /**
     * Sets the group by clause for this entity type, this clause should not
     * include the "group by" keywords.
     * @param groupByClause the group by clause
     * @return this {@link Entity.Definition} instance
     * @throws IllegalStateException in case a group by clause has already been set,
     * for example automatically, based on grouping properties
     * @see Property.ColumnProperty#setGroupingColumn(boolean)
     */
    Definition setGroupByClause(final String groupByClause);

    /**
     * @return the having clause to use when querying entities of this type,
     * without the "having" keyword
     */
    String getHavingClause();

    /**
     * Sets the having clause for this entity type, this clause should not
     * include the "having" keyword.
     * @param havingClause the having clause
     * @return this {@link Entity.Definition} instance
     * @throws IllegalStateException in case a having clause has already been set,
     * for example automatically, based on grouping properties
     */
    Definition setHavingClause(final String havingClause);

    /**
     * @return the name of the table to use when selecting entities of this type
     */
    String getSelectTableName();

    /**
     * Sets the name of the table to use when selecting entities of this type,
     * when it differs from the one used to update/insert, such as a view.
     * @param selectTableName the name of the table
     * @return this {@link Entity.Definition} instance
     */
    Definition setSelectTableName(final String selectTableName);

    /**
     * @return the select query to use when selecting entities of this type
     */
    String getSelectQuery();

    /**
     * Sets the select query to use when selecting entities of this type,
     * use with care. The order of the properties when defining the entity
     * must match the column order in the given query.
     * @param selectQuery the select query to use for this entity type
     * @param containsWhereClause true if the given query contains a where clause
     * @return this {@link Entity.Definition} instance
     */
    Definition setSelectQuery(final String selectQuery, final boolean containsWhereClause);

    /**
     * @return true if the select query, if any, contains a where clause
     */
    boolean selectQueryContainsWhereClause();

    /**
     * @return the object responsible for providing toString values for this entity type
     */
    ToString getStringProvider();

    /**
     * Sets the string provider, that is, the object responsible for providing toString values for this entity type
     * @param stringProvider the string provider
     * @return this {@link Entity.Definition} instance
     */
    Definition setStringProvider(final ToString stringProvider);

    /**
     * Sets the comparator to use when comparing this entity type to other entities
     * @param comparator the comparator
     * @return this {@link Entity.Definition} instance
     */
    Definition setComparator(final Comparator<Entity> comparator);

    /**
     * @return the comparator used when comparing this entity type to other entities
     */
    Comparator<Entity> getComparator();

    /**
     * @return a collection of property IDs identifying the properties to use when performing
     * a default lookup for this entity type
     */
    Collection<String> getSearchPropertyIDs();

    /**
     * Sets the IDs of the properties to use when performing a default lookup for this entity type
     * @param searchPropertyIDs the search property IDs
     * @return this {@link Entity.Definition} instance
     */
    Definition setSearchPropertyIDs(final String... searchPropertyIDs);

    /**
     * @return the properties for this entity type
     */
    Map<String, Property> getProperties();

    /**
     * @return true if this entity contains any properties which values are linked to other properties
     */
    boolean hasDerivedProperties();

    /**
     * Returns true if this entity contains properties which values are derived from the value of the given property
     * @param propertyID the ID of the property
     * @return true if any properties are derived from the given property
     */
    boolean hasDerivedProperties(final String propertyID);

    /**
     * Returns the properties which values are derived from the value of the given property,
     * an empty collection if no such derived properties exist
     * @param propertyID the ID of the property
     * @return a collection containing the properties which are derived from the given property
     */
    Collection<Property.DerivedProperty> getDerivedProperties(final String propertyID);

    /**
     * @return the primary key properties of this entity type, sorted by primary key column index
     */
    List<Property.ColumnProperty> getPrimaryKeyProperties();

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
    Object getBackgroundColor(final Entity entity, final Property property);
  }

  /**
   * Annotation for entityID domain model fields, containing database related information about the entity
   */
  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Table {

    /**
     * @return the underlying table name
     */
    String tableName() default "";

    /**
     * @return the table or view to use when selecting entites
     */
    String selectTableName() default "";

    /**
     * @return the query to use when selecting entities
     */
    String selectQuery() default "";

    /**
     * @return true if the the select query contains a where clause
     */
    boolean selectQueryContainsWhereClause() default false;

    /**
     * @return the order by clause to use by default
     */
    String orderByClause() default "";

    /**
     * @return the having clause to use
     */
    String havingClause() default "";

    /**
     * @return the key generator type
     */
    Entity.KeyGenerator.Type keyGenerator() default KeyGenerator.Type.NONE;

    /**
     * @return the id source for the key generator
     */
    String keyGeneratorSource() default "";

    /**
     * @return the column to use in case of the auto increment key generator
     */
    String keyGeneratorIncrementColumnName() default "";
  }
}