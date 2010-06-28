/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.petstore.domain.Petstore;

/**
 * User: Bjorn Darri
 * Date: 30.12.2007
 * Time: 22:54:36
 */
public class AddressModel extends DefaultEntityModel {

  public AddressModel(final EntityDbProvider dbProvider) {
    super(Petstore.T_ADDRESS, dbProvider);
  }
}
