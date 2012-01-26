/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.ColumnSearchModel;
import org.jminor.framework.domain.Property;

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
