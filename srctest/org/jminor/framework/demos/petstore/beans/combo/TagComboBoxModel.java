/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.demos.petstore.beans.combo;

import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.demos.petstore.model.Petstore;

public class TagComboBoxModel extends EntityComboBoxModel {

  public TagComboBoxModel(final IEntityDbProvider dbProvider) {
    super(dbProvider, Petstore.T_TAG);
  }
}