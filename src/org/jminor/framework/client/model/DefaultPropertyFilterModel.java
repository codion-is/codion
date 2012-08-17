/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.table.DefaultColumnSearchModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

/**
 * A class for filtering a set of entities based on a property.
 */
@SuppressWarnings({"unchecked"})
public class DefaultPropertyFilterModel extends DefaultColumnSearchModel<Property> {

  /**
   * Instantiates a new DefaultPropertyFilterModel
   * @param property the property
   */
  public DefaultPropertyFilterModel(final Property property) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER),
            property.getFormat());
  }

  /** {@inheritDoc} */
  @Override
  protected final Comparable getComparable(final Object object) {
    final Entity entity = (Entity) object;
    if (entity.isValueNull(getColumnIdentifier().getPropertyID())) {
      return null;
    }

    final Property property = getColumnIdentifier();
    final Object value = entity.getValue(property.getPropertyID());
    if (property instanceof Property.ForeignKeyProperty) {
      return value.toString();
    }
    else {
      return (Comparable) value;
    }
  }
}