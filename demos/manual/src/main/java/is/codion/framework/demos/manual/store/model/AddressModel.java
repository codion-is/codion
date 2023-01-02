/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.swing.framework.model.SwingEntityModel;

// tag::addressModel[]
public class AddressModel extends SwingEntityModel {

  public AddressModel(EntityConnectionProvider connectionProvider) {
    super(Address.TYPE, connectionProvider);
  }
}
// end::addressModel[]