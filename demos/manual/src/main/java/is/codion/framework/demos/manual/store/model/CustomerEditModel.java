/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.swing.framework.model.SwingEntityEditModel;

// tag::customerEditModel[]
public class CustomerEditModel extends SwingEntityEditModel {

  public CustomerEditModel(EntityConnectionProvider connectionProvider) {
    super(Customer.TYPE, connectionProvider);
  }
}
// end::customerEditModel[]