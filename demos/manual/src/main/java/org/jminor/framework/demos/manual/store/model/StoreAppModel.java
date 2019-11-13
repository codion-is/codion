/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;

public class StoreAppModel extends SwingEntityApplicationModel {

  public StoreAppModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);

    CustomerModel customerModel = new CustomerModel(connectionProvider);
    AddressModel addressModel = new AddressModel(connectionProvider);

    customerModel.addDetailModel(addressModel);

    addEntityModel(customerModel);
  }
}
