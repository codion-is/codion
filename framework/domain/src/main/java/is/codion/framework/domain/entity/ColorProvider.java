/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.domain.entity;

import dev.codion.framework.domain.property.Property;

import java.io.Serializable;

/**
 * Provides background colors for entities.
 */
public interface ColorProvider extends Serializable {

  /**
   * @param entity the entity
   * @param property the property
   * @return the color to use for this entity and property
   */
  Object getColor(Entity entity, Property property);
}
