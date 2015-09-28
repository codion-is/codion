/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.swing.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Property;

/**
 * Specifies an object responsible for providing property search models
 */
public interface PropertySearchModelProvider {

  /**
   * Initializes a PropertySearchModel for the given property
   * @param property the Property for which to create a PropertySearchModel
   * @param connectionProvider the EntityConnectionProvider instance to use in case the property is a ForeignKeyProperty
   * @return a PropertySearchModel for the given property, null if this property is not searchable or if searching
   * should not be allowed for this property
   */
  PropertySearchModel<? extends Property.SearchableProperty> initializePropertySearchModel(
          final Property.SearchableProperty property, final EntityConnectionProvider connectionProvider);
}
