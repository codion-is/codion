/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.manual.store.model;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.demos.manual.store.domain.Store;
import dev.codion.swing.framework.model.SwingEntityEditModel;

// tag::customerEditModel[]
public class CustomerEditModel extends SwingEntityEditModel {

  public CustomerEditModel(EntityConnectionProvider connectionProvider) {
    super(Store.T_CUSTOMER, connectionProvider);
  }
}
// end::customerEditModel[]