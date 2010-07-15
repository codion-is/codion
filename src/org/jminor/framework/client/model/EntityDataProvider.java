/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.provider.EntityDbProvider;

/**
 * User: darri<br>
 * Date: 15.7.2010<br>
 * Time: 11:28:45
 */
public interface EntityDataProvider {

  /**
   * @return the ID of the entity this data provider is based on
   */
  String getEntityID();

  /**
   * @return the db provider this data provider uses
   */
  EntityDbProvider getDbProvider();
}
