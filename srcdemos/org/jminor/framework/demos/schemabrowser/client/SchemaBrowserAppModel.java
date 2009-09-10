/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.client;

import org.jminor.common.db.User;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.EntityDbProvider;
import org.jminor.framework.demos.schemabrowser.beans.SchemaModel;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;

import java.util.Arrays;
import java.util.List;

public class SchemaBrowserAppModel extends EntityApplicationModel {

  public SchemaBrowserAppModel(final EntityDbProvider dbProvider) throws UserException {
    super(dbProvider);
  }

  public SchemaBrowserAppModel(final User user) throws UserException {
    super(user, SchemaBrowserAppModel.class.getSimpleName());
  }

  @Override
  protected List<? extends EntityModel> initializeMainApplicationModels(final EntityDbProvider dbProvider) throws UserException {
    return Arrays.asList(new SchemaModel(dbProvider));
  }

  /** {@inheritDoc} */
  @Override
  protected void loadDomainModel() {
    new SchemaBrowser();
  }
}
