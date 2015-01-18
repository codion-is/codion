/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.EntityConnectionProvider;

/**
 * The base interface for model objects responsible for reading and writing entities to and from a database.
 */
public interface EntityDataProvider {

  /**
   * @return the ID of the entity this data provider is based on
   */
  String getEntityID();

  /**
   * @return the connection provider used by this data provider
   */
  EntityConnectionProvider getConnectionProvider();
}
