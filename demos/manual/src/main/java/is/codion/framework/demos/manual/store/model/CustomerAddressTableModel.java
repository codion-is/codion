/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store.CustomerAddress;
import is.codion.swing.framework.model.SwingEntityTableModel;

// tag::customerAddressTableModel[]
public class CustomerAddressTableModel extends SwingEntityTableModel {

  public CustomerAddressTableModel(EntityConnectionProvider connectionProvider) {
    super(CustomerAddress.TYPE, connectionProvider);
  }
}
// end::customerAddressTableModel[]