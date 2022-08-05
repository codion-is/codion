/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;

/**
 * A property representing a column that should get its value automatically from a column in a referenced table
 * @param <T> the underlying type
 */
public interface DenormalizedProperty<T> extends ColumnProperty<T> {

  /**
   * @return the attribute referencing the entity from which this property should retrieve its value
   */
  Attribute<Entity> entityAttribute();

  /**
   * @return the attribute in the referenced entity from which this property gets its value
   */
  Attribute<T> denormalizedAttribute();
}
