/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.table.DefaultColumnConditionModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

/**
 * A class for filtering a set of entities based on a property.
 */
public class DefaultPropertyFilterModel extends DefaultColumnConditionModel<Property> {

  /**
   * Instantiates a new DefaultPropertyFilterModel
   * @param property the property
   */
  public DefaultPropertyFilterModel(final Property property) {
    super(property, property.getTypeClass(), Property.WILDCARD_CHARACTER.get(), property.getFormat(), property.getDateTimeFormatPattern());
  }

  /** {@inheritDoc} */
  @Override
  protected final Comparable getComparable(final Object object) {
    final Entity entity = (Entity) object;
    if (entity.isValueNull(getColumnIdentifier())) {
      return null;
    }

    final Property property = getColumnIdentifier();
    final Object value = entity.get(property);
    if (value instanceof Entity) {
      return value.toString();
    }
    else {
      return (Comparable) value;
    }
  }
}