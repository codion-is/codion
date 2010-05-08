/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.framework.db.provider.EntityDbProvider;

import java.util.Collection;

public class PropertyValueListProvider implements ValueCollectionProvider<Object> {

  private final EntityDbProvider dbProvider;
  private final String entityID;
  private final String propertyID;

  public PropertyValueListProvider(final EntityDbProvider dbProvider, final String entityID, final String propertyID) {
    this.dbProvider = dbProvider;
    this.entityID = entityID;
    this.propertyID = propertyID;
  }

  public String getEntityID() {
    return entityID;
  }

  public String getPropertyID() {
    return propertyID;
  }

  public EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  public Collection<Object> getValues() {
    try {
      return getDbProvider().getEntityDb().selectPropertyValues(getEntityID(), getPropertyID(), true);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
