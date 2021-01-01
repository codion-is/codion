/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;

/**
 * Provides background colors for entities.
 */
public interface ColorProvider extends Serializable {

  /**
   * @param entity the entity
   * @param attribute the attribute
   * @return the color to use for this entity and attribute
   */
  Object getColor(Entity entity, Attribute<?> attribute);
}
