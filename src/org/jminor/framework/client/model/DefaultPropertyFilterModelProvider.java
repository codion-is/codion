/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.ColumnSearchModel;
import org.jminor.framework.domain.Property;

final class DefaultPropertyFilterModelProvider implements PropertyFilterModelProvider {

  public ColumnSearchModel<Property> initializePropertyFilterModel(final Property property) {
    return new DefaultPropertyFilterModel(property);
  }
}
