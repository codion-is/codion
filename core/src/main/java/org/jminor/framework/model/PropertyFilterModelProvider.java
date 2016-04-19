/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.framework.domain.Property;

/**
 * Specifies an object responsible for providing property filter models
 */
public interface PropertyFilterModelProvider {

  /**
   * Initializes a ColumnCriteriaModel for the given property
   * @param property the Property for which to initialize a ColumnCriteriaModel
   * @return a ColumnCriteriaModel for the given property
   */
  ColumnCriteriaModel<Property> initializePropertyFilterModel(final Property property);
}
