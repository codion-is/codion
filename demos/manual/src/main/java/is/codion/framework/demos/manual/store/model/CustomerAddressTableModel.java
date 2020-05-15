/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.manual.store.model;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.demos.manual.store.domain.Store;
import dev.codion.swing.framework.model.SwingEntityTableModel;

// tag::customerAddressTableModel[]
public class CustomerAddressTableModel extends SwingEntityTableModel {

  public CustomerAddressTableModel(final EntityConnectionProvider connectionProvider) {
    super(Store.T_CUSTOMER_ADDRESS, connectionProvider);
  }
}
// end::customerAddressTableModel[]