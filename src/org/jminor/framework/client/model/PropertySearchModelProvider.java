/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Property;

public interface PropertySearchModelProvider {

  /**
   * Initializes a PropertySearchModel for the given property
   * @param property the Property for which to create a PropertySearchModel
   * @param dbProvider the EntityDbProvider instance to use in case the property is a ForeignKeyProperty
   * @return a PropertySearchModel for the given property, null if this property is not searchable or if searching
   * should not be allowed for this property
   */
  PropertySearchModel<? extends Property.SearchableProperty> initializePropertySearchModel(
          final Property.SearchableProperty property, final EntityDbProvider dbProvider);
}
