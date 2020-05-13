/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.swing.framework.model.SwingEntityModel;

// tag::customerAddressModel[]
public class CustomerAddressModel extends SwingEntityModel {

  public CustomerAddressModel(EntityConnectionProvider connectionProvider) {
    super(new CustomerAddressTableModel(connectionProvider));
  }
}
// end::customerAddressModel[]