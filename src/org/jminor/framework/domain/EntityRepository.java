/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.IdSource;
import org.jminor.common.model.ValueMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A static repository for all Entity related meta-data
 */
public class EntityRepository {

  private static Map<String, EntityDefinition> entityDefinitions = new HashMap<String, EntityDefinition>();

  private EntityRepository() {}

  public static boolean isDefined(final String entityID) {
    return entityDefinitions.containsKey(entityID);
  }

  /**
   * @param entityID the entity ID
   * @return a String array containing the IDs of the properties used as default search properties
   * for entities identified by <code>entityID</code>
   */
  public static String[] getEntitySearchPropertyIDs(final String entityID) {
    final List<String> searchPropertyIDs = getEntityDefinition(entityID).getSearchPropertyIDs();
    if (searchPropertyIDs != null)
      return searchPropertyIDs.toArray(new String[searchPropertyIDs.size()]);

    return null;
  }

  /**
   * Retrieves the properties used when searching for a entity of the given type,
   * if no search property IDs are defined all STRING based properties are returned.
   * @param entityID the entity ID
   * @return the search properties to use
   */
  public static List<Property> getSearchProperties(final String entityID) {
    final String[] searchPropertyIds = getEntitySearchPropertyIDs(entityID);
    List<Property> searchProperties;
    if (searchPropertyIds != null) {
      searchProperties = getProperties(entityID, searchPropertyIds);
    }
    else {//use all string properties
      final Collection<Property> properties =
              getDatabaseProperties(entityID);
      searchProperties = new ArrayList<Property>();
      for (final Property property : properties)
        if (property.getPropertyType() == Type.STRING)
          searchProperties.add(property);
    }

    return searchProperties;
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the primary key properties of the entity identified by <code>entityID</code>
   */
  public static List<Property.PrimaryKeyProperty> getPrimaryKeyProperties(final String entityID) {
    return getEntityDefinition(entityID).getPrimaryKeyProperties();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by <code>entityID</code> is read only
   * @throws RuntimeException if the read only value is undefined
   */
  public static boolean isReadOnly(final String entityID) {
    return getEntityDefinition(entityID).isReadOnly();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by <code>entityID</code> is based on a large dataset
   * @throws RuntimeException if the large dataset value is undefined
   */
  public static boolean isLargeDataset(final String entityID) {
    return getEntityDefinition(entityID).isLargeDataset();
  }

  /**
   * @param entityID the entity ID
   * @return a comma separated list of columns to use in the order by clause
   */
  public static String getOrderByClause(final String entityID) {
    return getEntityDefinition(entityID).getOrderByClause();
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table used to select entities identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public static String getSelectTableName(final String entityID) {
    return getEntityDefinition(entityID).getSelectTableName();
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table on which entities identified by <code>entityID</code> are based
   * @throws RuntimeException if none is defined
   */
  public static String getTableName(final String entityID) {
    return getEntityDefinition(entityID).getTableName();
  }

  /**
   * @param entityID the entity ID
   * @return the sql query used when selecting entities identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public static String getSelectQuery(final String entityID) {
    return getEntityDefinition(entityID).getSelectQuery();
  }

  /**
   * @param entityID the entity ID
   * @return the query string used to select entities identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public static String getSelectColumnsString(final String entityID) {
    return getEntityDefinition(entityID).getSelectColumnsString();
  }

  /**
   * @param entityID the entity ID
   * @return the IdSource of the entity identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public static IdSource getIdSource(final String entityID) {
    return getEntityDefinition(entityID).getIdSource();
  }

  /**
   * @param entityID the entity ID
   * @return the StringProvider used in case toString() is called for the given entity
   * @throws RuntimeException if none is defined
   */
  public static ValueMap.ToString<String, Object> getStringProvider(final String entityID) {
    return getEntityDefinition(entityID).getStringProvider();
  }

  /**
   * Returns true if the value for the primary key of this entity is automatically generated, either by the framework,
   * such as values queried from sequences or set by triggers
   * @param entityID the entity ID
   * @return true if the value for the primary key is automatically generated
   */
  public static boolean isPrimaryKeyAutoGenerated(final String entityID) {
    return getIdSource(entityID).isAutoGenerated();
  }

  /**
   * Retrieves the database properties comprising the entity identified by <code>entityID</code>
   * @param entityID the entity ID
   * @param includePrimaryKeyProperties if true primary key properties are included
   * @param includeReadOnly if true then properties that are marked as 'read only' are included
   * @param includeNonUpdatable if true then non updatable properties are included
   * @return a list containing the database properties (properties that map to database columns) comprising
   * the entity identified by <code>entityID</code>
   */
  public static List<Property> getDatabaseProperties(final String entityID,
                                                     final boolean includePrimaryKeyProperties,
                                                     final boolean includeReadOnly,
                                                     final boolean includeNonUpdatable) {
    final List<Property> properties = new ArrayList<Property>(getDatabaseProperties(entityID));
    final ListIterator<Property> iterator = properties.listIterator();
    while (iterator.hasNext()) {
      final Property property = iterator.next();
      if (!includeReadOnly && property.isReadOnly() || !includeNonUpdatable && !property.isUpdatable()
              || !includePrimaryKeyProperties && property instanceof Property.PrimaryKeyProperty)
        iterator.remove();
    }

    return properties;
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the visible (non-hidden) properties
   * in the entity identified by <code>entityID</code>
   * @throws RuntimeException if no visible properties are defined for the given entity
   */
  public static List<Property> getVisibleProperties(final String entityID) {
    return getEntityDefinition(entityID).getVisibleProperties();
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the property identified by <code>propertyID</code> in the entity identified by <code>entityID</code>
   * @throws RuntimeException in case no such property exists
   */
  public static Property getProperty(final String entityID, final String propertyID) {
    final Property property = getProperties(entityID).get(propertyID);
    if (property == null)
      throw new RuntimeException("Property '" + propertyID + "' not found in entity: " + entityID);

    return property;
  }

  /**
   * @param entityID the entity ID
   * @param propertyIDs the property IDs of the properties to retrieve
   * @return a list containing the properties identified by <code>propertyIDs</code>, found in
   * the entity identified by <code>entityID</code>
   */
  public static List<Property> getProperties(final String entityID, final String... propertyIDs) {
    final List<Property> properties = new ArrayList<Property>();
    for (final String propertyID : propertyIDs)
      properties.add(getProperty(entityID, propertyID));

    return properties;
  }

  /**
   * @param entityID the entity ID
   * @param includeHidden true if hidden properties should be included in the result
   * @return a collection containing the properties found in the entity identified by <code>entityID</code>
   */
  public static Collection<Property> getProperties(final String entityID, final boolean includeHidden) {
    return includeHidden ? getProperties(entityID).values() : getVisibleProperties(entityID);
  }

  /**
   * @param entityID the entity ID
   * @return a collection containing all database properties found in the entity identified by <code>entityID</code>,
   * that is, properties that map to database columns
   */
  public static Collection<Property> getDatabaseProperties(final String entityID) {
    return getEntityDefinition(entityID).getDatabaseProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a collection containing all transient database properties found in the entity identified by <code>entityID</code>,
   * that is, properties that do not map to database columns
   */
  public static Collection<Property.TransientProperty> getTransientProperties(final String entityID) {
    return getEntityDefinition(entityID).getTransientProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a collection containing all the foreign key properties found in the entity
   * identified by <code>entityID</code>
   */
  public static Collection<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID) {
    return getEntityDefinition(entityID).getForeignKeyProperties();
  }

  /**
   * @param entityID the entity ID
   * @return true if the given entity contains denormalized properties
   */
  public static boolean hasDenormalizedProperties(final String entityID) {
    return getEntityDefinition(entityID).hasDenormalizedProperties();
  }

  /**
   * Returns true if the property identified by <code>propertyID</code> in the entity identified
   * by <code>entityID</code> has any linked transient properties, that is transient properties which
   * values depend on the value of the given property
   * @param entityID the entityID
   * @param propertyID the propertyID
   * @return true if any transient properties are linked to the given property
   */
  public static boolean hasLinkedTransientProperties(final String entityID, final String propertyID) {
    return getEntityDefinition(entityID).hasLinkedProperties(propertyID);
  }

  /**
   * Returns the IDs of any transient properties linked to the property identified by <code>propertyID</code>
   * in the entity identified by <code>entityID</code>
   * @param entityID the entityID
   * @param propertyID the propertyID
   * @return the IDs of any transient properties that are linked to the given property
   */
  public static Collection<String> getLinkedTransientPropertyIDs(final String entityID, final String propertyID) {
    return getEntityDefinition(entityID).getLinkedPropertyIDs(propertyID);
  }

  /**
   * @param entityID the entity ID
   * @param propertyOwnerEntityID the entity ID of the actual property owner entity
   * @return a collection containing all denormalized properties of the entity identified by <code>entityID</code>
   * which source is the entity identified by <code>propertyOwnerEntityID</code>
   */
  public static Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String entityID,
                                                                                    final String propertyOwnerEntityID) {
    return getEntityDefinition(entityID).getDenormalizedProperties(propertyOwnerEntityID);
  }

  /**
   * Returns the foreign key properties referencing entities of the given type
   * @param entityID the ID of the entity from which to retrieve the foreign key properties
   * @param referenceEntityID the ID of the reference entity
   * @return a List containing the properties, an empty list is returned in case no properties fit the criteria
   */
  public static List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID, final String referenceEntityID) {
    final List<Property.ForeignKeyProperty> properties = new ArrayList<Property.ForeignKeyProperty>();
    for (final Property.ForeignKeyProperty foreignKeyProperty : getForeignKeyProperties(entityID))
      if (foreignKeyProperty.getReferencedEntityID().equals(referenceEntityID))
        properties.add(foreignKeyProperty);

    return properties;
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the Property.ForeignKeyProperty with the given propertyID
   * @throws RuntimeException in case no such property exists
   */
  public static Property.ForeignKeyProperty getForeignKeyProperty(final String entityID, final String propertyID) {
    for (final Property.ForeignKeyProperty foreignKeyProperty : getForeignKeyProperties(entityID))
      if (foreignKeyProperty.is(propertyID))
        return foreignKeyProperty;

    throw new RuntimeException("Foreign key property with id: " + propertyID + " not found in entity of type: " + entityID);
  }

  /**
   * @param entityID the entity ID
   * @return a map containing the properties the given entity is comprised of, mapped to their respective propertyIDs
   */
  public static Map<String, Property> getProperties(final String entityID) {
    return getEntityDefinition(entityID).getProperties();
  }

  /**
   * @param entityID the entity ID
   * @return the name of the primary key value source for the given entity
   * @throws RuntimeException in case no id source name is specified
   */
  public static String getEntityIdSource(final String entityID) {
    return getEntityDefinition(entityID).getIdValueSource();
  }

  public static Collection<String> getDefinedEntities() {
    return new ArrayList<String>(entityDefinitions.keySet());
  }

  public static Map<String, EntityDefinition> getEntityDefinitions() {
    return entityDefinitions;
  }

  /**
   * @param entityGroup a group of related entities (from the same domain fx), for which
   * we can deduce that if one has been initialized all have.
   * @return true if any one of the entities in the group have already initialized, hmm?
   */
  public static boolean contains(final Map<String, EntityDefinition> entityGroup) {
    return entityGroup.size() == 0 || entityDefinitions.containsKey(entityGroup.keySet().iterator().next());
  }

  /**
   * Adds the given entityDefinition to this EntityRepository
   * @param entityDefinition the EntityDefinition to add
   */
  public static void add(EntityDefinition entityDefinition) {
    if (entityDefinitions.containsKey(entityDefinition.getEntityID()))
      throw new RuntimeException("Entity already added: " + entityDefinition.getEntityID());

    entityDefinitions.put(entityDefinition.getEntityID(), entityDefinition);
  }

  /**
   * Returns the EntityDefinition object associated with <code>entityID</code>
   * @param entityID the entityID
   * @return the EntityDefinition for the given entityID
   * @throws RuntimeException in case the entity has not been defined
   */
  private static EntityDefinition getEntityDefinition(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID);
  }
}