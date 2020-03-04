/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;

// tag::storeAppModel[]
public class StoreAppModel extends SwingEntityApplicationModel {

  public StoreAppModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);

    CustomerModel customerModel = new CustomerModel(connectionProvider);
    CustomerAddressModel customerAddressModel = new CustomerAddressModel(connectionProvider);

    customerModel.addDetailModel(customerAddressModel);

    addEntityModel(customerModel);
  }
}
// end::storeAppModel[]