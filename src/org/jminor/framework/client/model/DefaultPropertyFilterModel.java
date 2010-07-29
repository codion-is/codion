/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.DefaultSearchModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

/**
 * A class for filtering a set of entities based on a property.
 */
@SuppressWarnings({"unchecked"})
public class DefaultPropertyFilterModel extends DefaultSearchModel<Property> {

  public DefaultPropertyFilterModel(final Property property) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
  }

  @Override
  protected Comparable getComparable(final Object object) {
    final Entity entity = (Entity) object;
    if (entity.isValueNull(getSearchKey().getPropertyID())) {
      return null;
    }

    final Object value = entity.getValue(getSearchKey().getPropertyID());
    if (getSearchKey().isReference()) {
      return value.toString();
    }
    else {
      return (Comparable) value;
    }
  }
}