/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.petstore.beans.combo.ItemComboBoxModel;
import org.jminor.framework.demos.petstore.beans.combo.TagComboBoxModel;
import org.jminor.framework.demos.petstore.model.Petstore;
import org.jminor.framework.model.Property;

import javax.swing.ComboBoxModel;
import java.util.Map;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 23:34:22
 */
public class TagItemModel extends EntityModel {

  public TagItemModel(final IEntityDbProvider dbProvider) throws UserException {
    super("Item Tags", dbProvider, Petstore.T_TAG_ITEM);
    getTableModel().setFilterQueryByMaster(true);
  }

  /** {@inheritDoc} */
  protected Map<Property, ComboBoxModel> initializeEntityComboBoxModels() {
    return super.initializeEntityComboBoxModels(
            new ItemComboBoxModel(getDbConnectionProvider()),
            new TagComboBoxModel(getDbConnectionProvider()));
  }
}
