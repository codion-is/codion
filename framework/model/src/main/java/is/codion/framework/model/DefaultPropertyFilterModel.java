/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;

/**
 * A class for filtering a set of entities based on a property.
 */
public final class DefaultPropertyFilterModel<T> extends DefaultColumnConditionModel<Entity, Property<?>, T> {

  /**
   * Instantiates a new DefaultPropertyFilterModel
   * @param property the property
   */
  public DefaultPropertyFilterModel(final Property<T> property) {
    super(property, property.getAttribute().getTypeClass(), Property.WILDCARD_CHARACTER.get(), property.getFormat(), property.getDateTimeFormatPattern());
  }

  @Override
  protected Comparable<T> getComparable(final Entity row) {
    if (row.isNull(getColumnIdentifier().getAttribute())) {
      return null;
    }

    final Property<?> property = getColumnIdentifier();
    final Object value = row.get(property.getAttribute());
    if (value instanceof Entity) {
      return (Comparable<T>) value.toString();
    }

    return (Comparable<T>) value;
  }
}