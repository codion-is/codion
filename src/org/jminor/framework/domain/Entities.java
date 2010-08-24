/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.IdSource;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueMap;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.awt.Color;
import java.text.Collator;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A Entity factory class
 */
public final class Entities {

  private static final String ENTITY_ID_PARAM = "entityID";
  private static final Map<String, EntityDefinition> ENTITY_DEFINITIONS = new HashMap<String, EntityDefinition>();
  private static volatile Map<String, Proxy> proxies;

  private Entities() {}

  /**
   * Creates a new Entity instance with the given entityID
   * @param entityID the entity ID
   * @return a new Entity instance
   */
  public static Entity entityInstance(final String entityID) {
    return new EntityImpl(entityID);
  }

  /**
   * Creates a new Entity instance with the given primary key
   * @param key the primary key
   * @return a new Entity instance
   */
  public static Entity entityInstance(final Entity.Key key) {
    return new EntityImpl(key);
  }

  /**
   * Creates a new Entity instance with the given entityID and the given values/originalValues
   * @param entityID the entity ID
   * @param values the values
   * @param originalValues the original values
   * @return a new Entity instance
   */
  public static Entity entityInstance(final String entityID, final Map<String, Object> values, final Map<String, Object> originalValues) {
    return EntityImpl.entityInstance(entityID, values, originalValues);
  }

  /**
   * Creates a new Entity.Key instance with the given entityID
   * @param entityID the entity ID
   * @return a new Entity.Key instance
   */
  public static Entity.Key keyInstance(final String entityID) {
    return new EntityImpl.KeyImpl(entityID);
  }

  /**
   * Defines a new entity, by default the <code>entityID</code> is used as the underlying table name
   * @param entityID the ID uniquely identifying the entity
   * @param propertyDefinitions the Property objects to base this entity on
   * @return a new EntityDefinition
   */
  public static EntityDefinition define(final String entityID, final Property... propertyDefinitions) {
    return define(entityID, entityID, propertyDefinitions);
  }

  /**
   * Defines a new entity
   * @param entityID the ID uniquely identifying the entity
   * @param tableName the name of the underlying table
   * @param propertyDefinitions the Property objects to base the entity on
   * @return a new EntityDefinition
   */
  public static EntityDefinition define(final String entityID, final String tableName, final Property... propertyDefinitions) {
    final EntityDefinition definition = new EntityDefinitionImpl(entityID, tableName, propertyDefinitions);
    add(definition);

    return definition;
  }

  /**
   * @param entityID the entity ID
   * @return true if an entity with the given ID has been defined
   */
  public static boolean isDefined(final String entityID) {
    return ENTITY_DEFINITIONS.containsKey(entityID);
  }

  /**
   * @param domainID the domain ID
   * @return all entity IDs associated with the given domain
   */
  public static Collection<String> getDomainEntityIDs(final String domainID) {
    final Collection<String> entityIDs = new ArrayList<String>();
    for (final EntityDefinition definition : ENTITY_DEFINITIONS.values()) {
      if (definition.getDomainID().equals(domainID)) {
        entityIDs.add(definition.getEntityID());
      }
    }

    return entityIDs;
  }

  /**
   * @param entityID the entity ID
   * @return a String array containing the IDs of the properties used as default search properties
   * for entities identified by <code>entityID</code>
   */
  public static Collection<String> getEntitySearchPropertyIDs(final String entityID) {
    return getEntityDefinition(entityID).getSearchPropertyIDs();
  }

  /**
   * Retrieves the properties used when searching for a entity of the given type,
   * if no search property IDs are defined all STRING based properties are returned.
   * @param entityID the entity ID
   * @return the search properties to use
   */
  public static List<Property.ColumnProperty> getSearchProperties(final String entityID) {
    return getSearchProperties(entityID, getEntitySearchPropertyIDs(entityID));
  }

  /**
   * Retrieves the properties used when searching for a entity of the given type,
   * if no search property IDs are defined all STRING based properties are returned.
   * @param entityID the entity ID
   * @param searchPropertyIds the IDs of the properties to use as search properties
   * @return the search properties to use
   */
  public static List<Property.ColumnProperty> getSearchProperties(final String entityID, final Collection<String> searchPropertyIds) {
    final List<Property.ColumnProperty> searchProperties = new ArrayList<Property.ColumnProperty>();
    if (searchPropertyIds != null && !searchPropertyIds.isEmpty()) {
      for (final String propertyID : searchPropertyIds) {
        searchProperties.add(getColumnProperty(entityID, propertyID));
      }
    }
    else {
      for (final Property.ColumnProperty property : getColumnProperties(entityID)) {
        if (property.isString() && property.isSearchable()) {
          searchProperties.add(property);
        }
      }
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
  public static ValueMap.ToString<String> getStringProvider(final String entityID) {
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
   * Retrieves the column properties comprising the entity identified by <code>entityID</code>
   * @param entityID the entity ID
   * @param includePrimaryKeyProperties if true primary key properties are included
   * @param includeReadOnly if true then properties that are marked as 'read only' are included
   * @param includeNonUpdatable if true then non updatable properties are included
   * @return a list containing the database properties (properties that map to database columns) comprising
   * the entity identified by <code>entityID</code>
   */
  public static List<Property.ColumnProperty> getColumnProperties(final String entityID,
                                                                  final boolean includePrimaryKeyProperties,
                                                                  final boolean includeReadOnly,
                                                                  final boolean includeNonUpdatable) {
    return getColumnProperties(entityID, includePrimaryKeyProperties, includeReadOnly, includeNonUpdatable, true);
  }

  /**
   * Retrieves the column properties comprising the entity identified by <code>entityID</code>
   * @param entityID the entity ID
   * @param includePrimaryKeyProperties if true primary key properties are included
   * @param includeReadOnly if true then properties that are marked as 'read only' are included
   * @param includeNonUpdatable if true then non updatable properties are included
   * @param includeForeignKeyProperties if true then foreign key properties are included
   * @return a list containing the database properties (properties that map to database columns) comprising
   * the entity identified by <code>entityID</code>
   */
  public static List<Property.ColumnProperty> getColumnProperties(final String entityID,
                                                                  final boolean includePrimaryKeyProperties,
                                                                  final boolean includeReadOnly,
                                                                  final boolean includeNonUpdatable,
                                                                  final boolean includeForeignKeyProperties) {
    return getColumnProperties(entityID, includePrimaryKeyProperties, includeReadOnly, includeNonUpdatable, includeForeignKeyProperties, true);
  }

  /**
   * Retrieves the column properties comprising the entity identified by <code>entityID</code>
   * @param entityID the entity ID
   * @param includePrimaryKeyProperties if true primary key properties are included
   * @param includeReadOnly if true then properties that are marked as 'read only' are included
   * @param includeNonUpdatable if true then non updatable properties are included
   * @param includeForeignKeyProperties if true then foreign key properties are included
   * @param includeTransientProperties if true then transient properties are included
   * @return a list containing the database properties (properties that map to database columns) comprising
   * the entity identified by <code>entityID</code>
   */
  public static List<Property.ColumnProperty> getColumnProperties(final String entityID,
                                                                  final boolean includePrimaryKeyProperties,
                                                                  final boolean includeReadOnly,
                                                                  final boolean includeNonUpdatable,
                                                                  final boolean includeForeignKeyProperties,
                                                                  final boolean includeTransientProperties) {
    final List<Property.ColumnProperty> properties = new ArrayList<Property.ColumnProperty>(getColumnProperties(entityID));
    final ListIterator<Property.ColumnProperty> iterator = properties.listIterator();
    while (iterator.hasNext()) {
      final Property.ColumnProperty property = iterator.next();
      if (!includeReadOnly && property.isReadOnly()
              || !includeNonUpdatable && !property.isUpdatable()
              || !includePrimaryKeyProperties && property instanceof Property.PrimaryKeyProperty
              || !includeForeignKeyProperties && property instanceof Property.ForeignKeyProperty
              || !includeTransientProperties && property instanceof Property.TransientProperty) {
        iterator.remove();
      }
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
    Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
    return getEntityDefinition(entityID).getVisibleProperties();
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the column property identified by property ID
   */
  public static Property.ColumnProperty getColumnProperty(final String entityID, final String propertyID) {
    final Property property = getProperty(entityID, propertyID);
    if (!(property instanceof Property.ColumnProperty)) {
      throw new RuntimeException(propertyID + ", " + property.getClass() + " does not implement Property.ColumnProperty");
    }

    return (Property.ColumnProperty) property;
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the property identified by <code>propertyID</code> in the entity identified by <code>entityID</code>
   * @throws RuntimeException in case no such property exists
   */
  public static Property getProperty(final String entityID, final String propertyID) {
    Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
    Util.rejectNullValue(propertyID, "propertyID");
    final Property property = getProperties(entityID).get(propertyID);
    if (property == null) {
      throw new RuntimeException("Property '" + propertyID + "' not found in entity: " + entityID);
    }

    return property;
  }

  /**
   * @param entityID the entity ID
   * @param propertyIDs the property IDs of the properties to retrieve
   * @return a list containing the properties identified by <code>propertyIDs</code>, found in
   * the entity identified by <code>entityID</code>
   */
  public static List<Property> getProperties(final String entityID, final Collection<String> propertyIDs) {
    Util.rejectNullValue(propertyIDs, "propertyIDs");
    return getProperties(entityID, propertyIDs.toArray(new String[propertyIDs.size()]));
  }

  /**
   * @param entityID the entity ID
   * @param propertyIDs the property IDs of the properties to retrieve
   * @return a list containing the properties identified by <code>propertyIDs</code>, found in
   * the entity identified by <code>entityID</code>
   */
  public static List<Property> getProperties(final String entityID, final String... propertyIDs) {
    Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
    Util.rejectNullValue(propertyIDs, "propertyIDs");
    final List<Property> properties = new ArrayList<Property>();
    for (final String propertyID : propertyIDs) {
      properties.add(getProperty(entityID, propertyID));
    }

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
  public static Collection<Property.ColumnProperty> getColumnProperties(final String entityID) {
    return getEntityDefinition(entityID).getColumnProperties();
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
   * @param entityID the entity ID
   * @param foreignKeyPropertyID the foreign key id
   * @return a collection containing all denormalized properties of the entity identified by <code>entityID</code>
   * which source is the entity identified by <code>propertyOwnerEntityID</code>
   */
  public static Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String entityID,
                                                                                    final String foreignKeyPropertyID) {
    return getEntityDefinition(entityID).getDenormalizedProperties(foreignKeyPropertyID);
  }

  /**
   * @param entityID the entity ID
   * @param foreignKeyPropertyID the foreign key id
   * @return true if the entity identified by <code>entityID</code> contains denormalized properties
   * which source is the entity identified by <code>propertyOwnerEntityID</code>
   */
  public static boolean hasDenormalizedProperties(final String entityID, final String foreignKeyPropertyID) {
    return getEntityDefinition(entityID).hasDenormalizedProperties(foreignKeyPropertyID);
  }

  /**
   * Returns true if the property identified by <code>propertyID</code> in the entity identified
   * by <code>entityID</code> has any linked properties, that is properties which
   * values depend on the value of the given property
   * @param entityID the entityID
   * @param propertyID the propertyID
   * @return true if any derived properties are linked to the given property
   */
  public static boolean hasLinkedProperties(final String entityID, final String propertyID) {
    return getEntityDefinition(entityID).hasLinkedProperties(propertyID);
  }

  /**
   * Returns the IDs of any properties which values are linked to the property identified by <code>propertyID</code>
   * in the entity identified by <code>entityID</code>
   * @param entityID the entityID
   * @param propertyID the propertyID
   * @return the IDs of any properties which values are linked to the given property
   */
  public static Collection<String> getLinkedPropertyIDs(final String entityID, final String propertyID) {
    return getEntityDefinition(entityID).getLinkedPropertyIDs(propertyID);
  }

  /**
   * Returns the foreign key properties referencing entities of the given type
   * @param entityID the ID of the entity from which to retrieve the foreign key properties
   * @param referenceEntityID the ID of the reference entity
   * @return a List containing the properties, an empty list is returned in case no properties fit the criteria
   */
  public static List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID, final String referenceEntityID) {
    final List<Property.ForeignKeyProperty> properties = new ArrayList<Property.ForeignKeyProperty>();
    for (final Property.ForeignKeyProperty foreignKeyProperty : getForeignKeyProperties(entityID)) {
      if (foreignKeyProperty.getReferencedEntityID().equals(referenceEntityID)) {
        properties.add(foreignKeyProperty);
      }
    }

    return properties;
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the Property.ForeignKeyProperty with the given propertyID
   * @throws RuntimeException in case no such property exists
   */
  public static Property.ForeignKeyProperty getForeignKeyProperty(final String entityID, final String propertyID) {
    for (final Property.ForeignKeyProperty foreignKeyProperty : getForeignKeyProperties(entityID)) {
      if (foreignKeyProperty.is(propertyID)) {
        return foreignKeyProperty;
      }
    }

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

  /**
   * @param entityID the entity ID
   * @return true if row coloring is enabled for the given entity type
   */
  public static boolean isRowColoring(final String entityID) {
    return getEntityDefinition(entityID).isRowColoring();
  }

  /**
   * @param entityID the entity ID
   * @return the caption associated with the given entity type
   */
  public static String getCaption(final String entityID) {
    return getEntityDefinition(entityID).getCaption();
  }

  /**
   * @return the entityIDs of all defined entities
   */
  public static Collection<String> getDefinedEntities() {
    return new ArrayList<String>(ENTITY_DEFINITIONS.keySet());
  }

  /**
   * @return a tree model showing the dependencies between entities via foreign keys
   */
  public static TreeModel getDependencyTreeModel() {
    return getDependencyTreeModel(null);
  }

  /**
   * @param domainID the ID of the domain for which to return a dependency tree model
   * @return a tree model showing the dependencies between entities via foreign keys
   */
  public static TreeModel getDependencyTreeModel(final String domainID) {
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);
    for (final String entityID : getEntityDefinitions(domainID).values()) {
      final EntityDefinition definition = ENTITY_DEFINITIONS.get(entityID);
      if (definition.getForeignKeyProperties().isEmpty() || referencesOnlySelf(definition)) {
        root.add(new EntityDependencyTreeNode(domainID, definition.getEntityID()));
      }
    }

    return new DefaultTreeModel(root);
  }

  /**
   * @return a map containing all defined entityIDs, with their respective table names as an associated value
   */
  public static Map<String, String> getEntityDefinitions() {
    return getEntityDefinitions(null);
  }

  /**
   * @param domainID the ID of the domain for which to retrieve the entity definitions
   * @return a map containing all defined entityIDs, with their respective table names as an associated value
   */
  public static Map<String, String> getEntityDefinitions(final String domainID) {
    final Map<String, String> definitions = new HashMap<String, String>();
    for (final EntityDefinition definition : ENTITY_DEFINITIONS.values()) {
      if (domainID == null) {
        definitions.put(definition.getEntityID(), definition.getTableName());
      }
      else {
        if (getDomainEntityIDs(domainID).contains(definition.getEntityID())) {
          definitions.put(definition.getEntityID(), definition.getTableName());
        }
      }
    }

    return definitions;
  }

  /**
   * Sets a entity specific proxy instance
   * @param entityID the ID of the entity for which this proxy instance is used
   * @param entityProxy the proxy instance to link to the given entity ID
   * @see Proxy
   */
  public static void setProxy(final String entityID, final Proxy entityProxy) {
    Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
    Util.rejectNullValue(entityProxy, "entityProxy");
    if (proxies == null) {
      proxies = new HashMap<String, Proxy>();
    }

    if (proxies.containsKey(entityID)) {
      throw new RuntimeException("Proxy already set for: " + entityID);
    }

    proxies.put(entityID, entityProxy);
  }

  /**
   * Returns the proxy instance assigned to the given entity ID or the default proxy if none has been assigned
   * @param entityID the entity ID for which to retrieve the proxy
   * @return the proxy instance assigned to the given entity ID
   * @see Proxy
   */
  public static Proxy getProxy(final String entityID) {
    Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
    if (proxies != null && proxies.containsKey(entityID)) {
      return proxies.get(entityID);
    }

    return Proxy.getInstance();
  }

  /**
   * Returns the EntityDefinition object associated with <code>entityID</code>
   * @param entityID the entityID
   * @return the EntityDefinition for the given entityID
   * @throws IllegalArgumentException in case the entity has not been defined
   */
  private static EntityDefinition getEntityDefinition(final String entityID) {
    Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
    final EntityDefinition definition = ENTITY_DEFINITIONS.get(entityID);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityID);
    }

    return definition;
  }

  /**
   * Adds the given entityDefinition to this Entities repository
   * @param entityDefinition the EntityDefinition to add
   */
  private static void add(final EntityDefinition entityDefinition) {
    Util.rejectNullValue(entityDefinition, "entityDefinition");
    if (ENTITY_DEFINITIONS.containsKey(entityDefinition.getEntityID())) {
      throw new RuntimeException("Entity already added: " + entityDefinition.getEntityID());
    }

    ENTITY_DEFINITIONS.put(entityDefinition.getEntityID(), entityDefinition);
  }

  /**
   * Acts as a proxy for retrieving values from Entity objects, allowing for plugged
   * in entity specific functionality, such as providing toString() and compareTo() implementations
   */
  public static class Proxy {

    private static final Proxy INSTANCE = new Proxy();

    private final Collator collator = Collator.getInstance();

    static Proxy getInstance() {
      return INSTANCE;
    }

    /**
     * Compares the given entities.
     * @param entity the first entity
     * @param entityToCompare the second entity
     * @return the compare result
     */
    public int compareTo(final Entity entity, final Entity entityToCompare) {
      Util.rejectNullValue(entity, "entity");
      Util.rejectNullValue(entityToCompare, "entityToCompare");
      return collator.compare(entity.toString(), entityToCompare.toString());
    }

    /**
     * @param entity the entity
     * @return a string representation of the given entity
     */
    public String toString(final Entity entity) {
      Util.rejectNullValue(entity, "entity");
      final String entityID = entity.getEntityID();
      final ValueMap.ToString<String> stringProvider = getStringProvider(entityID);

      if (stringProvider == null) {
        return new StringBuilder(entityID).append(": ").append(entity.getPrimaryKey()).toString();
      }

      return stringProvider.toString(entity);
    }

    /**
     * @param entity the entity
     * @param property the derived property
     * @return the derived property value
     */
    public Object getDerivedValue(final Entity entity, final Property.DerivedProperty property) {
      throw new RuntimeException("getDerivedValue() has not been overriden in Entity.Proxy for: " + entity + ", " + property);
    }

    /**
     * @param entity the entity
     * @param property the property
     * @param format the format
     * @return a formatted version of the value associated with the given property
     */
    public String getFormattedValue(final Entity entity, final Property property, final Format format) {
      Util.rejectNullValue(entity, "entity");
      final Object value = entity.getValue(property);
      if (value == null) {
        return "";
      }

      if (format == null) {
        return value.toString();
      }

      return format.format(value);
    }

    /**
     * @param entity the entity
     * @return the background color to use for this entity
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public Color getBackgroundColor(final Entity entity) {
      return null;
    }
  }

  private static boolean referencesOnlySelf(final EntityDefinition definition) {
    for (final Property.ForeignKeyProperty fkProperty : definition.getForeignKeyProperties()) {
      if (!fkProperty.getReferencedEntityID().equals(definition.getEntityID())) {
        return false;
      }
    }

    return true;
  }

  private static final class EntityDependencyTreeNode extends DefaultMutableTreeNode {

    private final String domainID;

    private EntityDependencyTreeNode(final String domainID, final String entityID) {
      super(entityID);
      this.domainID = domainID;
      Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
    }

    /**
     * @return the ID of the entity this node represents
     */
    public String getEntityID() {
      return (String) getUserObject();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return getEntityID();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return getEntityID().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
      return obj instanceof EntityDependencyTreeNode && getEntityID().equals(((EntityDependencyTreeNode) obj).getEntityID());
    }

    /** {@inheritDoc} */
    @Override
    public void setParent(final MutableTreeNode newParent) {
      super.setParent(newParent);
      removeAllChildren();
      for (final EntityDependencyTreeNode child : initializeChildren()) {
        add(child);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void setUserObject(final Object userObject) {
      if (!(userObject instanceof String)) {
        throw new IllegalArgumentException("entityID required, got: " + userObject);
      }
      super.setUserObject(userObject);
    }

    private List<EntityDependencyTreeNode> initializeChildren() {
      final List<EntityDependencyTreeNode> childrenList = new ArrayList<EntityDependencyTreeNode>();
      for (final String entityID : getEntityDefinitions(domainID).keySet()) {
        for (final Property.ForeignKeyProperty fkProperty : getForeignKeyProperties(entityID)) {
          if (fkProperty.getReferencedEntityID().equals(getEntityID()) && !foreignKeyCycle(fkProperty.getReferencedEntityID())) {
            childrenList.add(new EntityDependencyTreeNode(domainID, entityID));
          }
        }
      }

      return childrenList;
    }

    private boolean foreignKeyCycle(final String referencedEntityID) {
      TreeNode tmp = getParent();
      while (tmp instanceof EntityDependencyTreeNode) {
        if (((EntityDependencyTreeNode) tmp).getEntityID().equals(referencedEntityID)) {
          return true;
        }
        tmp = tmp.getParent();
      }

      return false;
    }
  }
}
