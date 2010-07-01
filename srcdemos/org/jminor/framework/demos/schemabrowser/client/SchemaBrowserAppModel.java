/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.client;

import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.schemabrowser.beans.SchemaModel;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;

public class SchemaBrowserAppModel extends EntityApplicationModel {

  public SchemaBrowserAppModel(final EntityDbProvider dbProvider) {
    super(dbProvider);
    addMainApplicationModel(new SchemaModel(dbProvider));
  }

  @Override
  protected void loadDomainModel() {
    new SchemaBrowser();
  }
}
