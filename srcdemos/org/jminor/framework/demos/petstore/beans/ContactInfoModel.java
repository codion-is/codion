/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.petstore.domain.Petstore;

import java.util.Arrays;
import java.util.List;

/**
 * User: Bjorn Darri
 * Date: 30.12.2007
 * Time: 22:54:36
 */
public class ContactInfoModel extends EntityModel {

  public ContactInfoModel(final EntityDbProvider dbProvider) {
    super(Petstore.T_SELLER_CONTACT_INFO, dbProvider);
  }

  @Override
  protected List<? extends EntityModel> initializeDetailModels() {
    return Arrays.asList(new ItemModel(getDbProvider()));
  }
}