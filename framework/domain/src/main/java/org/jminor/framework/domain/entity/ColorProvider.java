/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity;

import org.jminor.framework.domain.property.Property;

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
