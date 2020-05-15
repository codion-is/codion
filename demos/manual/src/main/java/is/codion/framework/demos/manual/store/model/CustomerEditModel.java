/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.swing.framework.model.SwingEntityEditModel;

// tag::customerEditModel[]
public class CustomerEditModel extends SwingEntityEditModel {

  public CustomerEditModel(EntityConnectionProvider connectionProvider) {
    super(Store.T_CUSTOMER, connectionProvider);
  }
}
// end::customerEditModel[]