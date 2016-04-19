/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Property;

/**
 * Specifies an object responsible for providing property criteria models
 */
public interface PropertyCriteriaModelProvider {

  /**
   * Initializes a PropertyCriteriaModel for the given property
   * @param property the Property for which to create a PropertyCriteriaModel
   * @param connectionProvider the EntityConnectionProvider instance to use in case the property is a ForeignKeyProperty
   * @return a PropertyCriteriaModel for the given property, null if this property is not searchable or if searching
   * should not be allowed for this property
   */
  PropertyCriteriaModel<? extends Property.SearchableProperty> initializePropertyCriteriaModel(
          final Property.SearchableProperty property, final EntityConnectionProvider connectionProvider);
}
