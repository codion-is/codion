/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;

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