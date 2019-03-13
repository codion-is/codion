/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;

/**
 * The base interface for objects responsible for reading and/or writing entities to and from a database.
 */
public interface EntityDataProvider {

  /**
   * @return the ID of the entity this data provider is based on
   */
  String getEntityId();

  /**
   * @return the connection provider used by this data provider
   */
  EntityConnectionProvider getConnectionProvider();
}
