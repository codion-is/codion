/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.swing.framework.model.SwingEntityModel;

public class AddressModel extends SwingEntityModel {

  public AddressModel(EntityConnectionProvider connectionProvider) {
    super(new AddressEditModel(connectionProvider),
            new AddressTableModel(connectionProvider));
  }
}
