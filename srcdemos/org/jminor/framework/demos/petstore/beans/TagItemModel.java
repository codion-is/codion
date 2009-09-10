/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.EntityDbProvider;
import org.jminor.framework.demos.petstore.domain.Petstore;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 23:34:22
 */
public class TagItemModel extends EntityModel {

  public TagItemModel(final EntityDbProvider dbProvider) throws UserException {
    super(Petstore.T_TAG_ITEM, dbProvider);
  }
}
