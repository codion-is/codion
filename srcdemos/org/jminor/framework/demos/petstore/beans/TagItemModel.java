/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.petstore.domain.Petstore;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 23:34:22
 */
public class TagItemModel extends EntityModel {

  public TagItemModel(final EntityDbProvider dbProvider) {
    super(Petstore.T_TAG_ITEM, dbProvider);
  }
}
