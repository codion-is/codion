/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

import static is.codion.framework.demos.petstore.domain.Petstore.*;

public final class PetstoreAppModel extends SwingEntityApplicationModel {

  public PetstoreAppModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    final SwingEntityModel categoryModel = new SwingEntityModel(Category.TYPE, connectionProvider);
    final SwingEntityModel productModel = new SwingEntityModel(Product.TYPE, connectionProvider);
    final SwingEntityModel itemModel = new SwingEntityModel(Item.TYPE, connectionProvider);
    final SwingEntityModel tagItemModel = new SwingEntityModel(TagItem.TYPE, connectionProvider);
    itemModel.addDetailModels(tagItemModel);
    productModel.addDetailModels(itemModel);
    categoryModel.addDetailModels(productModel);
    addEntityModel(categoryModel);
  }
}
