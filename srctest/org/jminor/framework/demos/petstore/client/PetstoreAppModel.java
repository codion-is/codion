/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.client;

import org.jminor.common.db.User;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.petstore.beans.CategoryModel;
import org.jminor.framework.demos.petstore.model.Petstore;

import java.util.List;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 14:08:33
 */
public class PetstoreAppModel extends EntityApplicationModel {

  public PetstoreAppModel(final IEntityDbProvider dbProvider) throws UserException {
    super(dbProvider);
  }

  public PetstoreAppModel (final User user) throws UserException {
    super(user, PetstoreAppModel.class.getSimpleName());
  }

  /** {@inheritDoc} */
  protected List<Class<? extends EntityModel>> getRootEntityModelClasses() throws UserException {
    return EntityModel.asList(CategoryModel.class);
  }

  /** {@inheritDoc} */
  protected void loadDomainModel() {
    new Petstore();
  }
}
