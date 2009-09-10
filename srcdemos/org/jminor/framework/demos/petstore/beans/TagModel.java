/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.EntityDbProvider;
import org.jminor.framework.demos.petstore.domain.Petstore;

import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 14:02:41
 */
public class TagModel extends EntityModel {

  public TagModel(final EntityDbProvider dbProvider) throws UserException {
    super(Petstore.T_TAG, dbProvider);
  }

  /** {@inheritDoc} */
  @Override
  protected List<? extends EntityModel> initializeDetailModels() throws UserException {
    return Arrays.asList(new TagItemModel(getDbProvider()));
  }
}