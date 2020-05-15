/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.swing.framework.model.SwingEntityModel;

// tag::addressModel[]
public class AddressModel extends SwingEntityModel {

  public AddressModel(EntityConnectionProvider connectionProvider) {
    super(Store.T_ADDRESS, connectionProvider);
  }
}
// end::addressModel[]