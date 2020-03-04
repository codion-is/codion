/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.swing.framework.model.SwingEntityEditModel;

// tag::customerEditModel[]
public class CustomerEditModel extends SwingEntityEditModel {

  public CustomerEditModel(EntityConnectionProvider connectionProvider) {
    super(Store.T_CUSTOMER, connectionProvider);
  }
}
// end::customerEditModel[]