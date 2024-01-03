/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;

// tag::customerAddressModel[]
public class CustomerAddressModel extends SwingEntityModel {

  public CustomerAddressModel(EntityConnectionProvider connectionProvider) {
    super(new CustomerAddressTableModel(connectionProvider));
  }
}
// end::customerAddressModel[]