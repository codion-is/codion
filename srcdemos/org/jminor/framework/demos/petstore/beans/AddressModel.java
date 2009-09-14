/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.petstore.domain.Petstore;

/**
 * User: Björn Darri
 * Date: 30.12.2007
 * Time: 22:54:36
 */
public class AddressModel extends EntityModel {

  public AddressModel(final EntityDbProvider dbProvider) throws UserException {
    super(Petstore.T_ADDRESS, dbProvider);
  }
}
