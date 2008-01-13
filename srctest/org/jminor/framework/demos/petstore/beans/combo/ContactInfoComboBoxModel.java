/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.combo;

import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.petstore.model.Petstore;

public class ContactInfoComboBoxModel extends EntityComboBoxModel {

  public ContactInfoComboBoxModel(final IEntityDbProvider dbProvider) {
    super(dbProvider, Petstore.T_SELLER_CONTACT_INFO);
  }
}