/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.petstore.model;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.swing.framework.model.SwingEntityApplicationModel;
import dev.codion.swing.framework.model.SwingEntityModel;

import static dev.codion.framework.demos.petstore.domain.Petstore.*;

public final class PetstoreAppModel extends SwingEntityApplicationModel {

  public PetstoreAppModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    final SwingEntityModel categoryModel = new SwingEntityModel(T_CATEGORY, connectionProvider);
    final SwingEntityModel productModel = new SwingEntityModel(T_PRODUCT, connectionProvider);
    final SwingEntityModel itemModel = new SwingEntityModel(T_ITEM, connectionProvider);
    final SwingEntityModel tagItemModel = new SwingEntityModel(T_TAG_ITEM, connectionProvider);
    itemModel.addDetailModels(tagItemModel);
    productModel.addDetailModels(itemModel);
    categoryModel.addDetailModels(productModel);
    addEntityModel(categoryModel);
  }
}
