/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;

/**
 * A class for filtering a set of entities based on a property.
 * @param <T> the underlying column value type
 */
public final class DefaultPropertyFilterModel<T> extends DefaultColumnConditionModel<Entity, Attribute<?>, T> {

  /**
   * Instantiates a new DefaultPropertyFilterModel
   * @param property the property
   */
  public DefaultPropertyFilterModel(final Property<T> property) {
    super(property.getAttribute(), property.getAttribute().getTypeClass(), Property.WILDCARD_CHARACTER.get(),
            property.getFormat(), property.getDateTimePattern());
    setComparableFunction(row -> {
      if (row.isNull(property.getAttribute())) {
        return null;
      }

      final Object value = row.get(property.getAttribute());
      if (value instanceof Entity) {
        return (Comparable<T>) value.toString();
      }

      return (Comparable<T>) value;
    });
  }
}