/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.petstore.model.Petstore;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 23:34:22
 */
public class TagItemModel extends EntityModel {

  public TagItemModel(final IEntityDbProvider dbProvider) throws UserException {
    super("Item Tags", dbProvider, Petstore.T_TAG_ITEM);
    getTableModel().setFilterQueryByMaster(true);
  }
}
