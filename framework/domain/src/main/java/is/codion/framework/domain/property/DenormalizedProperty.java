/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;

/**
 * A property representing a column that should get its value automatically from a column in a referenced table
 * @param <T> the underlying type
 */
public interface DenormalizedProperty<T> extends ColumnProperty<T> {

  /**
   * @return the id of the foreign key property from which this property should retrieve its value
   */
  Attribute<Entity> getEntityAttribute();

  /**
   * @param <T> the underlying type
   * @return the property in the referenced entity from which this property gets its value
   */
  Attribute<T> getDenormalizedAttribute();
}
