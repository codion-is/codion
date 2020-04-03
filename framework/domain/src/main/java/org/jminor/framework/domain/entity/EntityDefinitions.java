/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity;

import org.jminor.framework.domain.property.Property;

/**
 * A factory for {@link EntityDefinition}s.
 */
public final class EntityDefinitions {

  private EntityDefinitions() {}

  /**
   * Instantiates a {@link EntityDefinition} instance and returns a {@link EntityDefinition.Builder} for configuring it.
   * @param definitionProvider the domain entity definition provider
   * @param entityId the entity id
   * @param tableName the underlying table name
   * @param propertyBuilders the builders for the properties
   * @return a {@link EntityDefinition.Builder} instance
   */
  public static EntityDefinition.Builder definition(final EntityDefinition.Provider definitionProvider, final String entityId,
                                                    final String tableName, final Property.Builder... propertyBuilders) {
    return new DefaultEntityDefinition(definitionProvider, entityId, tableName, propertyBuilders).builder();
  }
}
