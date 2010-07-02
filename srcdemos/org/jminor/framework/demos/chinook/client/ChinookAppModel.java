/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.client;

import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:15:12
 */
public class ChinookAppModel extends EntityApplicationModel {

  public ChinookAppModel(final EntityDbProvider dbProvider) {
    super(dbProvider);
  }

  @Override
  protected void loadDomainModel() {
    new Chinook();
  }
}
