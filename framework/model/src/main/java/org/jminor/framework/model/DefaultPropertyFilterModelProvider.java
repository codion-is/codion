/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.property.Property;

/**
 * A default PropertyFilterModelProvider implementation.
 */
public class DefaultPropertyFilterModelProvider implements PropertyFilterModelProvider {

  /** {@inheritDoc} */
  @Override
  public ColumnConditionModel<Property> initializePropertyFilterModel(final Property property) {
    return new DefaultPropertyFilterModel(property);
  }
}
