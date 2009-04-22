/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.petstore.model.Petstore;

/**
 * User: Bj�rn Darri
 * Date: 30.12.2007
 * Time: 22:54:36
 */
public class AddressModel extends EntityModel {

  public AddressModel(final IEntityDbProvider dbProvider) throws UserException {
    super("Address", dbProvider, Petstore.T_ADDRESS);
  }
}
