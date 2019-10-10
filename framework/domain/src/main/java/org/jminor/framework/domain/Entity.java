/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Configuration;
import org.jminor.common.PropertyValue;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.valuemap.ValueMap;
import org.jminor.common.db.valuemap.exception.LengthValidationException;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.RangeValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Format;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Represents a row in a database table, providing access to the column values via the {@link ValueMap} interface.
 */
public interface Entity extends ValueMap<Property, Object>, Comparable<Entity>, Serializable {

  /**
   * @return the entity ID
   */
  String getEntityId();

  /**
   * @return the primary key of this entity
   */
  Key getKey();

  /**
   * @return the primary key of this entity in its original state
   */
  Key getOriginalKey();

  /**
   * @return the type of key generator used for generating primary key values for entities of this type
   */
  KeyGenerator.Type getKeyGeneratorType();

  /**
   * Retrieves the property identified by propertyId from the entity repository
   * @param propertyId the ID of the property to retrieve
   * @return the property identified by propertyId
   * @throws IllegalArgumentException in case the property does not exist in this entity
   */
  Property getProperty(final String propertyId);

  /**
   * @return the properties comprising this entity
   */
  List<Property> getProperties();

  /**
   * @return the primary key properties of this entity type, sorted by primary key column index
   */
  List<Property.ColumnProperty> getPrimaryKeyProperties();

  /**
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the given property
   */
  Object get(final String propertyId);

  /**
   * @param propertyId the ID of the property for which to retrieve the original value
   * @return the original value of the given property
   */
  Object getOriginal(final String propertyId);

  /**
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a String
   * @throws ClassCastException if the value is not a String instance
   */
  String getString(final String propertyId);

  /**
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is an Integer
   * @throws ClassCastException if the value is not a Integer instance
   */
  Integer getInteger(final String propertyId);

  /**
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a Long
   * @throws ClassCastException if the value is not a Integer instance
   */
  Long getLong(final String propertyId);

  /**
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a Character
   * @throws ClassCastException if the value is not a Character instance
   */
  Character getCharacter(final String propertyId);

  /**
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a Double.
   * @throws ClassCastException if the value is not a Double instance
   * @see Property#getMaximumFractionDigits()
   */
  Double getDouble(final String propertyId);

  /**
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a BigDecimal.
   * @throws ClassCastException if the value is not a BigDecimal instance
   * @see Property#getMaximumFractionDigits()
   */
  BigDecimal getBigDecimal(final String propertyId);

  /**
   * @param propertyId the ID of the date property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a LocalTime
   * @throws ClassCastException if the value is not a LocalTime instance
   */
  LocalTime getTime(final String propertyId);

  /**
   * @param propertyId the ID of the date property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a LocalDate
   * @throws ClassCastException if the value is not a LocalDate instance
   */
  LocalDate getDate(final String propertyId);

  /**
   * @param propertyId the ID of the date property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a LocalDatetime
   * @throws ClassCastException if the value is not a LocalDateTime instance
   */
  LocalDateTime getTimestamp(final String propertyId);

  /**
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a Boolean
   * @throws ClassCastException if the value is not a Boolean instance
   */
  Boolean getBoolean(final String propertyId);

  /**
   * @param propertyId the ID of the property for which to retrieve the formatted value
   * @param format the format object
   * @return the value of the property identified by {@code propertyId}, formatted with {@code format}
   */
  String getFormatted(final String propertyId, final Format format);

  /**
   * @param property the property for which to retrieve the formatted value
   * @param format the format object
   * @return the value of the given property, formatted with {@code format}
   */
  String getFormatted(final Property property, final Format format);

  /**
   * This method returns a String representation of the value associated with the given property,
   * if the property has a format it is used.
   * @param propertyId the ID of the property for which to retrieve the value
   * @return a String representation of the value of {@code property}
   * @see #getFormatted(Property, java.text.Format)
   */
  String getAsString(final String propertyId);

  /**
   * Returns the Entity instance referenced by the given foreign key property.
   * If the underlying reference property contains a value, that is,
   * a foreign key value exists but the actual referenced entity has not
   * been loaded, an "empty" entity is returned, containing only the primary
   * key value. Null is returned only if the actual reference property is null.
   * @param foreignKeyPropertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is an Entity
   * @throws IllegalArgumentException if the property is not a foreign key property
   * @see #isLoaded(String)
   */
  Entity getForeignKey(final String foreignKeyPropertyId);

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
   * @param propertyId the ID of the property
   * @param value the value
   * @return the previous value
   * @throws IllegalArgumentException in case the value type does not fit the property
   */
  Object put(final String propertyId, final Object value);

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
   * @param propertyId the propertyId
   * @return true if the value associated with the given property has been modified
   */
  boolean isModified(final String propertyId);

  /**
   * Clears the primary key values from this entity,
   * current as well as original values if any
   */
  void clearKeyValues();

  /**
   * @param entityId the entityId
   * @return true if this entity is of the given type
   */
  boolean is(final String entityId);

  /**
   * @param entity the entity to compare to
   * @return true if all column property values (Property.ColumnProperty) are equal
   */
  boolean valuesEqual(final Entity entity);

  /**
   * Returns true if the entity referenced via the given foreign key property has been loaded
   * @param foreignKeyPropertyId the property id
   * @return true if the reference entity has been loaded
   */
  boolean isLoaded(final String foreignKeyPropertyId);

  /**
   * @param property the property for which to retrieve the background color
   * @return the background color to use when displaying this property in a table
   */
  Object getBackgroundColor(final Property property);

  /**
   * Reverts the value associated with the given property to its original value.
   * If the value has not been modified then calling this method has no effect.
   * @param propertyId the ID of the property for which to revert the value
   */
  void revert(final String propertyId);

  /**
   * Saves the value associated with the given key, that is, removes the original value.
   * If no original value exists calling this method has no effect.
   * @param propertyId the ID of the property for which to save the value
   */
  void save(final String propertyId);

  /**
   * Returns true if a null value is mapped to the given property.
   * @param propertyId the ID of the property
   * @return true if the value mapped to the given property is null
   */
  boolean isValueNull(final String propertyId);

  /**
   * Returns true if this Entity contains a value for the given property, that value can be null.
   * @param propertyId the propertyId
   * @return true if a value is mapped to this property
   */
  boolean containsKey(final String propertyId);

  /**
   * Removes the given property and value from this Entity along with the original value if any.
   * If no value is mapped to the given property, this method has no effect.
   * @param propertyId the ID of the property to remove
   * @return the previous value mapped to the given key
   */
  Object remove(final String propertyId);

  /**
   * A class representing a primary key.
   */
  interface Key extends ValueMap<Property.ColumnProperty, Object>, Serializable {

    /**
     * @return the entity ID
     */
    String getEntityId();

    /**
     * @return a List containing the properties comprising this key
     */
    List<Property.ColumnProperty> getProperties();

    /**
     * @return the number of properties comprising this key
     */
    int getPropertyCount();

    /**
     * @return true if this key contains no values or if it contains a null value for a non-nullable key property
     */
    boolean isNull();

    /**
     * Returns true if a null value is mapped to the given property.
     * @param propertyId the propertyId
     * @return true if the value mapped to the given property is null
     */
    boolean isValueNull(final String propertyId);

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
     * @param propertyId the propertyId
     * @param value the value to associate with the property
     * @return the previous value
     */
    Object put(final String propertyId, final Object value);

    /**
     * @param propertyId the propertyId
     * @return the value associated with the given property
     */
    Object get(final String propertyId);
  }

  /**
   * Generates primary key values for entities on insert.
   * KeyGenerators fall into two categories, one in which the primary key value is
   * fetched or generated before the record is inserted and one where the underlying database
   * automatically sets the primary key value on insert, f.ex. with a table trigger or identity columns.
   * Implementations should override either {@code beforeInsert()} or {@code afterInsert()}.
   * {@code isAutoIncrement()} returns true if the database generates primary key values automatically,
   * this implies that {@code afterInsert()} should be used, fetching the generated primary key value
   * and updating the entity instance accordingly.
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
       * or the key generator implementation
       */
      AUTOMATIC(false, true);

      private final boolean manual;
      private final boolean autoIncrement;

      Type(final boolean manual, final boolean autoIncrement) {
        this.manual = manual;
        this.autoIncrement = autoIncrement;
      }

      /**
       * @return true if the underlying database handles the key generation, f.ex. with
       * a autoIncrement column or a database trigger
       */
      public boolean isAutoIncrement() {
        return autoIncrement;
      }

      /**
       * @return true if the key value needs to be included in the Entity before insert
       */
      public boolean isManual() {
        return manual;
      }
    }

    /**
     * Prepares the given entity for insert, that is, generates and fetches any required primary key values
     * and populates the entitys primary key.
     * The default version does nothing, override to implement.
     * @param entity the entity to prepare
     * @param connection the connection to use
     * @throws SQLException in case of an exception
     */
    default void beforeInsert(final Entity entity, final DatabaseConnection connection) throws SQLException {/*for overriding*/}

    /**
     * Prepares the given entity after insert, that is, fetches automatically generated primary
     * key values and populates the entitys primary key.
     * The default version does nothing, override to implement.
     * @param entity the entity to prepare
     * @param connection the connection to use
     * @param insertStatement the insert statement
     * @throws SQLException in case of an exception
     */
    default void afterInsert(final Entity entity, final DatabaseConnection connection, final Statement insertStatement) throws SQLException {/*for overriding*/}

    /**
     * Specifies whether the insert statement should return the primary key column values via the resulting
     * {@link Statement#getGeneratedKeys()} resultSet, accessible in {@link #afterInsert(Entity, DatabaseConnection, Statement)}.
     * The default implementation returns false.
     * @return true if the primary key column values should be returned via the insert statement resultSet
     * @see java.sql.Connection#prepareStatement(String, String[])
     */
    default boolean returnPrimaryKeyValues() {
      return false;
    }

    /**
     * @return the key generator type
     */
    Type getType();
  }

  /**
   * Provides background colors for entities.
   */
  interface BackgroundColorProvider extends Serializable {

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
  interface Validator extends ValueMap.Validator<Property, Entity>, Serializable {

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
     * @see Property#setNullable(boolean)
     * @see Property#isNullable()
     */
    void performNullValidation(final Entity entity, final Property property) throws NullValidationException;

    /**
     * Performs a range validation on the given number based property
     * @param entity the entity
     * @param property the property
     * @throws RangeValidationException in case the value of the given property is outside the legal range
     * @see Property#setMax(double)
     * @see Property#setMin(double)
     */
    void performRangeValidation(final Entity entity, final Property property) throws RangeValidationException;

    /**
     * Performs a length validation on the given string based property
     * @param entity the entity
     * @param property the property
     * @throws LengthValidationException in case the length of the value of the given property
     * @see Property#setMaxLength(int)
     */
    void performLengthValidation(final Entity entity, final Property property) throws LengthValidationException;
  }

  /**
   * Describes an object responsible for providing String representations of entity instances
   */
  interface ToString extends Serializable {
    /**
     * Returns a string representation of the given entity
     * @param entity the entity
     * @return a string representation of the entity
     */
    String toString(final Entity entity);
  }

  /**
   * Specifies a order by clause
   */
  interface OrderBy extends Serializable {

    /**
     * Adds an 'ascending' order by for the given properties
     * @param propertyIds the property ids
     * @return this OrderBy instance
     */
    OrderBy ascending(final String... propertyIds);

    /**
     * Adds a 'descending' order by for the given properties
     * @param propertyIds the property ids
     * @return this OrderBy instance
     */
    OrderBy descending(final String... propertyIds);

    /**
     * @return an unmodifiable List containing the propertyIds and their respective order
     */
    List<OrderByProperty> getOrderByProperties();

    /**
     * Specifies a propertyId and the order
     */
    interface OrderByProperty extends Serializable {

      /**
       * @return the id of the property to order by
       */
      String getPropertyId();

      /**
       * @return true if this property should be ordered in 'descending' order
       */
      boolean isDescending();
    }
  }

  /**
   * Specifies a entity definition.
   */
  interface Definition extends Serializable {

    /**
     * Specifies that it should not be possible to define foreign keys referencing entities that have
     * not been defined, this can be disabled in case of entities with circular references<br>
     * Value type: Boolean<br>
     * Default value: true
     */
    PropertyValue<Boolean> STRICT_FOREIGN_KEYS = Configuration.booleanValue("jminor.domain.strictForeignKeys", true);

    /**
     * @return the entity ID
     */
    String getEntityId();

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
    String getDomainId();

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
     * Sets the primary key generator, if {@code keyGenerator} is null then a default no-op generator is used.
     * @param keyGenerator the primary key generator, null for a no-op generator
     * @return this {@link Entity.Definition} instance
     */
    Definition setKeyGenerator(final KeyGenerator keyGenerator);

    /**
     * @return the type of key generator used for generating primary key values for entities of this type
     */
    KeyGenerator.Type getKeyGeneratorType();

    /**
     * Sets the order by clause for this entity type.
     * @param orderBy the order by clause
     * @return this {@link Entity.Definition} instance
     */
    Definition setOrderBy(final OrderBy orderBy);

    /**
     * @return the default order by clause to use when querying entities of this type
     */
    OrderBy getOrderBy();

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
    Collection<String> getSearchPropertyIds();

    /**
     * Sets the IDs of the properties to use when performing a default lookup for this entity type
     * @param searchPropertyIds the search property IDs
     * @return this {@link Entity.Definition} instance
     */
    Definition setSearchPropertyIds(final String... searchPropertyIds);

    /**
     * @return the properties for this entity type mapped to propertyIds
     */
    Map<String, Property> getPropertyMap();

    /**
     * @return a unmodifiable list view of the properties
     */
    List<Property> getProperties();

    /**
     * @return true if this entity contains any properties which values are derived from other properties
     */
    boolean hasDerivedProperties();

    /**
     * Returns true if this entity contains properties which values are derived from the value of the given property
     * @param propertyId the ID of the property
     * @return true if any properties are derived from the given property
     */
    boolean hasDerivedProperties(final String propertyId);

    /**
     * Returns the properties which values are derived from the value of the given property,
     * an empty collection if no such derived properties exist
     * @param propertyId the ID of the property
     * @return a collection containing the properties which are derived from the given property
     */
    Collection<Property.DerivedProperty> getDerivedProperties(final String propertyId);

    /**
     * @return the primary key properties of this entity type, sorted by primary key column index
     */
    List<Property.ColumnProperty> getPrimaryKeyProperties();

    /**
     * @return a map containing the primary key properties mapped to their respective propertyIds
     */
    Map<String, Property.ColumnProperty> getPrimaryKeyPropertyMap();

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
     * @param foreignKeyPropertyId the ID of the foreign key property
     * @return true if this entity type has any denormalized properties associated with the give foreign key
     */
    boolean hasDenormalizedProperties(final String foreignKeyPropertyId);

    /**
     * Retrieves the denormalized properties which values originate from the entity referenced by the given foreign key property
     * @param foreignKeyPropertyId the foreign key property ID
     * @return a list containing the denormalized properties which values originate from the entity
     * referenced by the given foreign key property
     */
    List<Property.DenormalizedProperty> getDenormalizedProperties(final String foreignKeyPropertyId);

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
     * @return the background color to use for this entity and property, null if none is specified
     */
    Object getBackgroundColor(final Entity entity, final Property property);
  }
}