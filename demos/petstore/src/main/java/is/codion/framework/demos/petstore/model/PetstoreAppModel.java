/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

import static is.codion.framework.demos.petstore.domain.Petstore.*;

public final class PetstoreAppModel extends SwingEntityApplicationModel {

  public PetstoreAppModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    SwingEntityModel categoryModel = new SwingEntityModel(Category.TYPE, connectionProvider);
    SwingEntityModel productModel = new SwingEntityModel(Product.TYPE, connectionProvider);
    productModel.getEditModel().initializeComboBoxModels(Product.CATEGORY_FK);
    SwingEntityModel itemModel = new SwingEntityModel(Item.TYPE, connectionProvider);
    itemModel.getEditModel().initializeComboBoxModels(Item.PRODUCT_FK, Item.CONTACT_INFO_FK, Item.ADDRESS_FK);
    SwingEntityModel tagItemModel = new SwingEntityModel(TagItem.TYPE, connectionProvider);
    tagItemModel.getEditModel().initializeComboBoxModels(TagItem.ITEM_FK, TagItem.TAG_FK);
    categoryModel.getTableModel().refresh();
    itemModel.addDetailModels(tagItemModel);
    productModel.addDetailModels(itemModel);
    categoryModel.addDetailModels(productModel);
    addEntityModel(categoryModel);
  }
}
