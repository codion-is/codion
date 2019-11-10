/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.property.Property;

/**
 * Specifies an object responsible for providing property filter models
 */
public interface PropertyFilterModelProvider {

  /**
   * Initializes a ColumnConditionModel for the given property
   * @param property the Property for which to initialize a ColumnConditionModel
   * @return a ColumnConditionModel for the given property
   */
  ColumnConditionModel<Property> initializePropertyFilterModel(final Property property);
}
