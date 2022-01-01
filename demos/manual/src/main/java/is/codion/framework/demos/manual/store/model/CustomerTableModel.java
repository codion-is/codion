/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.swing.framework.model.SwingEntityTableModel;

// tag::customerTableModel[]
public class CustomerTableModel extends SwingEntityTableModel {

  public CustomerTableModel(EntityConnectionProvider connectionProvider) {
    super(Customer.TYPE, connectionProvider);
  }
}
// end::customerTableModel[]