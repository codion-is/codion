/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.manual.store.model;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.swing.framework.model.SwingEntityModel;

// tag::customerAddressModel[]
public class CustomerAddressModel extends SwingEntityModel {

  public CustomerAddressModel(EntityConnectionProvider connectionProvider) {
    super(new CustomerAddressTableModel(connectionProvider));
  }
}
// end::customerAddressModel[]