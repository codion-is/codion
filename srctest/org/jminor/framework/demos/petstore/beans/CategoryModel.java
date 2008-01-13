/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.petstore.model.Petstore;

import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 14:02:41
 */
public class CategoryModel extends EntityModel {

  public CategoryModel(final IEntityDbProvider dbProvider) throws UserException {
    super("Category", dbProvider, Petstore.T_CATEGORY);
  }

  /** {@inheritDoc} */
  protected List<? extends EntityModel> initializeDetailModels() throws UserException {
    return Arrays.asList(new ProductModel(getDbConnectionProvider()));
  }
}
