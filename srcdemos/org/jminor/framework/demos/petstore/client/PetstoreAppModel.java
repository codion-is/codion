/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.client;

import org.jminor.common.db.User;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.petstore.beans.CategoryModel;
import org.jminor.framework.demos.petstore.domain.Petstore;

import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 14:08:33
 */
public class PetstoreAppModel extends EntityApplicationModel {

  public PetstoreAppModel(final EntityDbProvider dbProvider) {
    super(dbProvider);
  }

  public PetstoreAppModel (final User user) {
    super(user, PetstoreAppModel.class.getSimpleName());
  }

  @Override
  protected List<? extends EntityModel> initializeMainApplicationModels(final EntityDbProvider dbProvider) {
    return Arrays.asList(new CategoryModel(dbProvider));
  }

  @Override
  protected void loadDomainModel() {
    new Petstore();
  }
}
