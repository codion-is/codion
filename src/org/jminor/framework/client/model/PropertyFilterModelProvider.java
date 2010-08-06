/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.ColumnSearchModel;
import org.jminor.framework.domain.Property;

public interface PropertyFilterModelProvider {

  /**
   * Initializes a PropertyFilterModel for the given property
   * @param property the Property for which to initialize a PropertyFilterModel
   * @return a PropertyFilterModel for the given property
   */
  ColumnSearchModel<Property> initializePropertyFilterModel(final Property property);
}
