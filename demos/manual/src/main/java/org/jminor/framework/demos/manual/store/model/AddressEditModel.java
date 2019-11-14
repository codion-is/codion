/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.swing.framework.model.SwingEntityEditModel;

public class AddressEditModel extends SwingEntityEditModel {

  public AddressEditModel(EntityConnectionProvider connectionProvider) {
    super(Store.T_ADDRESS, connectionProvider);
  }
}
