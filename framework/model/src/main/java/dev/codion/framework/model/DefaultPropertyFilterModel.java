/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.table.DefaultColumnConditionModel;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.property.Property;

/**
 * A class for filtering a set of entities based on a property.
 */
public final class DefaultPropertyFilterModel extends DefaultColumnConditionModel<Entity, Property> {

  /**
   * Instantiates a new DefaultPropertyFilterModel
   * @param property the property
   */
  public DefaultPropertyFilterModel(final Property property) {
    super(property, property.getTypeClass(), Property.WILDCARD_CHARACTER.get(), property.getFormat(), property.getDateTimeFormatPattern());
  }

  @Override
  protected Comparable getComparable(final Entity row) {
    if (row.isNull(getColumnIdentifier())) {
      return null;
    }

    final Property property = getColumnIdentifier();
    final Object value = row.get(property);
    if (value instanceof Entity) {
      return value.toString();
    }

    return (Comparable) value;
  }
}