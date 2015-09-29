/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.domain.Property;
import org.jminor.swing.common.model.table.ColumnSearchModel;

/**
 * A default PropertyFilterModelProvider implementation.
 */
public class DefaultPropertyFilterModelProvider implements PropertyFilterModelProvider {

  /** {@inheritDoc} */
  @Override
  public ColumnSearchModel<Property> initializePropertyFilterModel(final Property property) {
    return new DefaultPropertyFilterModel(property);
  }
}
