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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.petstore.ui;

import is.codion.common.user.User;
import is.codion.demos.petstore.model.PetstoreAppModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.TabbedDetailLayout;

import java.util.List;
import java.util.Locale;

import static is.codion.demos.petstore.domain.Petstore.*;
import static is.codion.swing.framework.ui.EntityPanel.PanelState.HIDDEN;

public final class PetstoreAppPanel extends EntityApplicationPanel<PetstoreAppModel> {

	public PetstoreAppPanel(PetstoreAppModel applicationModel) {
		super(applicationModel, createPanels(applicationModel), createLookupPanelBuilders());
	}

	private static List<EntityPanel> createPanels(PetstoreAppModel applicationModel) {
		/* CATEGORY
		 *   PRODUCT
		 *     ITEM
		 *       ITEMTAG
		 */
		SwingEntityModel categoryModel = applicationModel.entityModels().get(Category.TYPE);
		SwingEntityModel productModel = categoryModel.detailModels().get(Product.TYPE);
		SwingEntityModel itemModel = productModel.detailModels().get(Item.TYPE);
		SwingEntityModel tagItemModel = itemModel.detailModels().get(TagItem.TYPE);

		EntityPanel categoryPanel = new EntityPanel(categoryModel,
						new CategoryEditPanel(categoryModel.editModel()),
						config -> config.detailLayout(entityPanel -> TabbedDetailLayout.builder()
										.panel(entityPanel)
										.splitPaneResizeWeight(0.3)
										.build()));
		EntityPanel productPanel = new EntityPanel(productModel,
						new ProductEditPanel(productModel.editModel()));
		EntityPanel itemPanel = new EntityPanel(itemModel,
						new ItemEditPanel(itemModel.editModel()),
						config -> config.detailLayout(entityPanel -> TabbedDetailLayout.builder()
										.panel(entityPanel)
										.initialDetailState(HIDDEN)
										.build()));
		EntityPanel tagItemPanel = new EntityPanel(tagItemModel,
						new TagItemEditPanel(tagItemModel.editModel()));

		categoryPanel.detailPanels().add(productPanel);
		productPanel.detailPanels().add(itemPanel);
		itemPanel.detailPanels().add(tagItemPanel);

		return List.of(categoryPanel);
	}

	private static List<EntityPanel.Builder> createLookupPanelBuilders() {
		return List.of(
						EntityPanel.builder(Address.TYPE,
										PetstoreAppPanel::createAddressPanel),
						EntityPanel.builder(SellerContactInfo.TYPE,
										PetstoreAppPanel::createSellerContactInfoPanel),
						EntityPanel.builder(Tag.TYPE,
										PetstoreAppPanel::createTagPanel));
	}

	private static EntityPanel createAddressPanel(EntityConnectionProvider connectionProvider) {
		SwingEntityModel addressModel = new SwingEntityModel(Address.TYPE, connectionProvider);
		addressModel.tableModel().items().refresh();

		return new EntityPanel(addressModel, new AddressEditPanel(addressModel.editModel()));
	}

	private static EntityPanel createSellerContactInfoPanel(EntityConnectionProvider connectionProvider) {
		SwingEntityModel sellerContactInfoModel = new SwingEntityModel(SellerContactInfo.TYPE, connectionProvider);
		sellerContactInfoModel.tableModel().items().refresh();

		return new EntityPanel(sellerContactInfoModel,
						new ContactInfoEditPanel(sellerContactInfoModel.editModel()));
	}

	private static EntityPanel createTagPanel(EntityConnectionProvider connectionProvider) {
		SwingEntityModel tagModel = new SwingEntityModel(Tag.TYPE, connectionProvider);
		SwingEntityModel tagItemModel = new SwingEntityModel(TagItem.TYPE, connectionProvider);
		tagModel.detailModels().add(tagItemModel);
		tagModel.tableModel().items().refresh();

		EntityPanel tagPanel = new EntityPanel(tagModel,
						new TagEditPanel(tagModel.editModel()), config -> config
						.detailLayout(entityPanel -> TabbedDetailLayout.builder()
										.panel(entityPanel)
										.initialDetailState(HIDDEN)
										.build()));
		EntityPanel tagItemPanel = new EntityPanel(tagItemModel,
						new TagItemEditPanel(tagItemModel.editModel()));
		tagPanel.detailPanels().add(tagItemPanel);

		return tagPanel;
	}

	public static void main(String[] args) {
		Locale.setDefault(new Locale("en"));
		EntityPanel.Config.TOOLBAR_CONTROLS.set(true);
		EntityApplicationPanel.builder(PetstoreAppModel.class, PetstoreAppPanel.class)
						.domain(DOMAIN)
						.applicationName("The Pet Store")
						.defaultUser(User.parse("scott:tiger"))
						.start();
	}
}