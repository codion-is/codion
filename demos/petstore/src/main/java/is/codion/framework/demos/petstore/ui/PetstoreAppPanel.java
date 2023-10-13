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
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.common.user.User;
import is.codion.framework.demos.petstore.model.PetstoreAppModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityPanel.PanelState;
import is.codion.swing.framework.ui.TabbedPanelLayout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static is.codion.framework.demos.petstore.domain.Petstore.*;
import static is.codion.swing.framework.ui.TabbedPanelLayout.detailPanelState;

public final class PetstoreAppPanel extends EntityApplicationPanel<PetstoreAppModel> {

  public PetstoreAppPanel(PetstoreAppModel applicationModel) {
    super(applicationModel);
  }

  @Override
  protected List<EntityPanel> createEntityPanels() {
    /* CATEGORY
     *   PRODUCT
     *     ITEM
     *       ITEMTAG
     */
    SwingEntityModel categoryModel = applicationModel().entityModel(Category.TYPE);
    SwingEntityModel productModel = categoryModel.detailModel(Product.TYPE);
    SwingEntityModel itemModel = productModel.detailModel(Item.TYPE);
    SwingEntityModel tagItemModel = itemModel.detailModel(TagItem.TYPE);

    EntityPanel categoryPanel = new EntityPanel(categoryModel,
            new CategoryEditPanel(categoryModel.editModel()),
            TabbedPanelLayout.splitPaneResizeWeight(0.3));
    EntityPanel productPanel = new EntityPanel(productModel,
            new ProductEditPanel(productModel.editModel()));
    EntityPanel itemPanel = new EntityPanel(itemModel,
            new ItemEditPanel(itemModel.editModel()),
            detailPanelState(PanelState.HIDDEN));
    EntityPanel tagItemPanel = new EntityPanel(tagItemModel,
            new TagItemEditPanel(tagItemModel.editModel()));

    categoryPanel.addDetailPanel(productPanel);
    productPanel.addDetailPanel(itemPanel);
    itemPanel.addDetailPanels(tagItemPanel);

    return Collections.singletonList(categoryPanel);
  }

  @Override
  protected List<EntityPanel.Builder> createSupportEntityPanelBuilders() {
    SwingEntityModel.Builder tagModelBuilder =
            SwingEntityModel.builder(Tag.TYPE)
                    .detailModelBuilder(SwingEntityModel.builder(TagItem.TYPE));
    SwingEntityModel.Builder sellerContactInfoModelBuilder =
            SwingEntityModel.builder(SellerContactInfo.TYPE)
                    .detailModelBuilder(SwingEntityModel.builder(Item.TYPE)
                            .detailModelBuilder(SwingEntityModel.builder(TagItem.TYPE)));

    return Arrays.asList(
            EntityPanel.builder(Address.TYPE)
                    .editPanelClass(AddressEditPanel.class),
            EntityPanel.builder(sellerContactInfoModelBuilder)
                    .editPanelClass(ContactInfoEditPanel.class)
                    .detailPanelBuilder(EntityPanel.builder(Item.TYPE)
                            .editPanelClass(ItemEditPanel.class)
                            .detailPanelBuilder(EntityPanel.builder(TagItem.TYPE)
                                    .editPanelClass(TagItemEditPanel.class))
                            .panelLayout(detailPanelState(PanelState.HIDDEN))),
            EntityPanel.builder(tagModelBuilder)
                    .editPanelClass(TagEditPanel.class)
                    .detailPanelBuilder(EntityPanel.builder(TagItem.TYPE)
                            .editPanelClass(TagItemEditPanel.class))
                    .panelLayout(detailPanelState(PanelState.HIDDEN)));
  }

  public static void main(String[] args) {
    Locale.setDefault(new Locale("en"));
    EntityPanel.TOOLBAR_CONTROLS.set(true);
    EntityApplicationPanel.builder(PetstoreAppModel.class, PetstoreAppPanel.class)
            .applicationName("The Pet Store")
            .domainType(DOMAIN)
            .frameSize(Windows.screenSizeRatio(0.8))
            .defaultLoginUser(User.parse("scott:tiger"))
            .start();
  }
}