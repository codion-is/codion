/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import java.util.Map;

/**
 * User: Björn Darri<br>
 * Date: 17.7.2010<br>
 * Time: 12:11:29
 */
public final class Entities {

  private Entities() {}

  public static Entity entityInstance(final String entityID) {
    return new EntityImpl(entityID);
  }

  public static Entity entityInstance(final Entity.Key key) {
    return new EntityImpl(key);
  }

  public static Entity entityInstance(final String entityID, final Map<String, Object> values, final Map<String, Object> originalValues) {
    return EntityImpl.entityInstance(entityID, values, originalValues);
  }

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
