/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.manual.store.model;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.demos.manual.store.domain.Store;
import dev.codion.swing.framework.model.SwingEntityModel;

// tag::addressModel[]
public class AddressModel extends SwingEntityModel {

  public AddressModel(EntityConnectionProvider connectionProvider) {
    super(Store.T_ADDRESS, connectionProvider);
  }
}
// end::addressModel[]