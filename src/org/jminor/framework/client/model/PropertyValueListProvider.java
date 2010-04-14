/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.ValueListProvider;
import org.jminor.framework.db.provider.EntityDbProvider;

import java.util.List;

public class PropertyValueListProvider implements ValueListProvider<Object> {

  private final EntityDbProvider dbProvider;
  private final String entityID;
  private final String propertyID;

  public PropertyValueListProvider(final EntityDbProvider dbProvider, final String entityID, final String propertyID) {
    this.dbProvider = dbProvider;
    this.entityID = entityID;
    this.propertyID = propertyID;
  }

  public List<Object> getValueList() throws Exception {
    return dbProvider.getEntityDb().selectPropertyValues(entityID, propertyID, true);
  }
}
