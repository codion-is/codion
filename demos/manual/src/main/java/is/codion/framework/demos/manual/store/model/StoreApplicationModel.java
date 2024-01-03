/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;

// tag::storeAppModel[]
public class StoreApplicationModel extends SwingEntityApplicationModel {

  public StoreApplicationModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);

    CustomerModel customerModel = new CustomerModel(connectionProvider);
    CustomerAddressModel customerAddressModel = new CustomerAddressModel(connectionProvider);

    customerModel.addDetailModel(customerAddressModel);

    //populate the model with rows from the database
    customerModel.tableModel().refresh();

    addEntityModel(customerModel);
  }
}
// end::storeAppModel[]