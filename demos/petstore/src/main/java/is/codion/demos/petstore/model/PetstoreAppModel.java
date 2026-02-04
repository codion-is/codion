/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.petstore.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

import java.util.List;

import static is.codion.demos.petstore.domain.Petstore.*;

public final class PetstoreAppModel extends SwingEntityApplicationModel {

	public PetstoreAppModel(EntityConnectionProvider connectionProvider) {
		super(connectionProvider, List.of(createCategoryModel(connectionProvider)));
	}

	private static SwingEntityModel createCategoryModel(EntityConnectionProvider connectionProvider) {
		SwingEntityModel categoryModel = new SwingEntityModel(Category.TYPE, connectionProvider);
		SwingEntityModel productModel = new SwingEntityModel(Product.TYPE, connectionProvider);
		productModel.editModel().editor().comboBoxModels().initialize(Product.CATEGORY_FK);
		SwingEntityModel itemModel = new SwingEntityModel(Item.TYPE, connectionProvider);
		itemModel.editModel().editor().comboBoxModels().initialize(Item.PRODUCT_FK, Item.CONTACT_INFO_FK, Item.ADDRESS_FK);
		SwingEntityModel tagItemModel = new SwingEntityModel(TagItem.TYPE, connectionProvider);
		tagItemModel.editModel().editor().comboBoxModels().initialize(TagItem.ITEM_FK, TagItem.TAG_FK);
		categoryModel.tableModel().items().refresh();
		itemModel.detailModels().add(tagItemModel);
		productModel.detailModels().add(itemModel);
		categoryModel.detailModels().add(productModel);

		return categoryModel;
	}
}
