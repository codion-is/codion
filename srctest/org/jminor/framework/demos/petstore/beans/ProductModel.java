/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.petstore.beans.combo.CategoryComboBoxModel;
import org.jminor.framework.demos.petstore.model.Petstore;
import org.jminor.framework.model.Property;

import javax.swing.ComboBoxModel;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 14:02:41
 */
public class ProductModel extends EntityModel {

  public ProductModel(final IEntityDbProvider dbProvider) throws UserException {
    super("Product", dbProvider, Petstore.T_PRODUCT);
    getTableModel().setFilterQueryByMaster(true);
    getTableModel().setShowAllWhenNotFiltered(true);
  }

  /** {@inheritDoc} */
  protected List<? extends EntityModel> initializeDetailModels() throws UserException {
    return Arrays.asList(new ItemModel(getDbConnectionProvider()));
  }

  /** {@inheritDoc} */
  protected Map<Property, ComboBoxModel> initializeEntityComboBoxModels() {
    return super.initializeEntityComboBoxModels(new CategoryComboBoxModel(getDbConnectionProvider()));
  }
}