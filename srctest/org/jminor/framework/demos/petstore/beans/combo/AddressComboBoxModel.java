/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.combo;

import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.petstore.model.Petstore;

public class AddressComboBoxModel extends EntityComboBoxModel {

  public AddressComboBoxModel(final IEntityDbProvider dbProvider) {
    super(dbProvider, Petstore.T_ADDRESS);
  }
}