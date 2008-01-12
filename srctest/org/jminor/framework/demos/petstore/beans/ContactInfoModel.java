/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 *
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.petstore.model.Petstore;

import java.util.Arrays;
import java.util.List;

/**
 * User: Bj�rn Darri
 * Date: 30.12.2007
 * Time: 22:54:36
 */
public class ContactInfoModel extends EntityModel {

  public ContactInfoModel(final IEntityDbProvider dbProvider) throws UserException {
    super("Seller Contact Info", dbProvider, Petstore.T_SELLER_CONTACT_INFO);
  }

  /** {@inheritDoc} */
  protected List<? extends EntityModel> initializeDetailModels() throws UserException {
    return Arrays.asList(new ItemModel(getDbConnectionProvider()));
  }
}