/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import java.util.Map;

/**
 * A Entity factory class
 */
public final class Entities {

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
   * @param propertyDefinitions the Property objects this entity should encompass
   * @return a new EntityDefinition
   */
  public static EntityDefinition define(final String entityID, final Property... propertyDefinitions) {
    return define(entityID, entityID, propertyDefinitions);
  }

  /**
   * Defines a new entity
   * @param entityID the ID uniquely identifying the entity
   * @param tableName the name of the underlying table
   * @param propertyDefinitions the Property objects this entity should encompass
   * @return a new EntityDefinition
   */
  public static EntityDefinition define(final String entityID, final String tableName, final Property... propertyDefinitions) {
    return new EntityDefinitionImpl(entityID, tableName, propertyDefinitions);
  }
}
