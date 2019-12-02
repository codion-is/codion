/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.framework.domain.property.Property;

/**
 * A factory for {@link EntityDefinition}s.
 */
public final class EntityDefinitions {

  private EntityDefinitions() {}

  /**
   * Instantiates a {@link EntityDefinition} instance and returns a {@link EntityDefinition.Builder}
   * for configuring it
   * @param domainId the domain id
   * @param entityId the entity id
   * @param tableName the underlying table name
   * @param propertyBuilders the builders for the properties
   * @return a {@link EntityDefinition.Builder} instance
   */
  public static EntityDefinition.Builder definition(final String domainId, final String entityId, final String tableName,
                                                    final Property.Builder... propertyBuilders) {
    return new DefaultEntityDefinition(domainId, entityId, tableName, propertyBuilders).builder();
  }
}
