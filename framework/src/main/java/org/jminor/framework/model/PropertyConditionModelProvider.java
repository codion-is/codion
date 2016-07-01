/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Property;

/**
 * Specifies an object responsible for providing property condition models
 */
public interface PropertyConditionModelProvider {

  /**
   * Initializes a PropertyConditionModel for the given property
   * @param property the Property for which to create a PropertyConditionModel
   * @param connectionProvider the EntityConnectionProvider instance to use in case the property is a ForeignKeyProperty
   * @return a PropertyConditionModel for the given property, null if this property is not searchable or if searching
   * should not be allowed for this property
   */
  PropertyConditionModel<? extends Property.SearchableProperty> initializePropertyConditionModel(
          final Property.SearchableProperty property, final EntityConnectionProvider connectionProvider);
}
