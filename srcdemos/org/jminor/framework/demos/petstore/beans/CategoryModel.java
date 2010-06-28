/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.petstore.domain.Petstore;

/**
 * User: Bjorn Darri
 * Date: 24.12.2007
 * Time: 14:02:41
 */
public class CategoryModel extends DefaultEntityModel {

  public CategoryModel(final EntityDbProvider dbProvider) {
    super(Petstore.T_CATEGORY, dbProvider);
    addDetailModel(new ProductModel(getDbProvider()));
  }
}
