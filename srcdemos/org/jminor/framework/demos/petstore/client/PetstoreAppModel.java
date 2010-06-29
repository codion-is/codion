/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.client;

import org.jminor.common.model.User;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.petstore.beans.CategoryModel;
import org.jminor.framework.demos.petstore.domain.Petstore;

/**
 * User: Bjorn Darri
 * Date: 24.12.2007
 * Time: 14:08:33
 */
public class PetstoreAppModel extends EntityApplicationModel {

  public PetstoreAppModel(final EntityDbProvider dbProvider) {
    super(dbProvider);
    addMainApplicationModel(new CategoryModel(dbProvider));
  }

  public PetstoreAppModel (final User user) {
    super(user, PetstoreAppModel.class.getSimpleName());
  }

  @Override
  protected void loadDomainModel() {
    new Petstore();
  }
}
