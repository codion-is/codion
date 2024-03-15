/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.common.user.User;
import is.codion.framework.demos.petstore.model.PetstoreAppModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityPanel.PanelState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static is.codion.framework.demos.petstore.domain.Petstore.*;
import static is.codion.swing.framework.ui.TabbedPanelLayout.detailPanelState;
import static is.codion.swing.framework.ui.TabbedPanelLayout.splitPaneResizeWeight;

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
            settings -> settings.panelLayout(splitPaneResizeWeight(0.3)));
    EntityPanel productPanel = new EntityPanel(productModel,
            new ProductEditPanel(productModel.editModel()));
    EntityPanel itemPanel = new EntityPanel(itemModel,
            new ItemEditPanel(itemModel.editModel()),
            settings -> settings.panelLayout(detailPanelState(PanelState.HIDDEN)));
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
                    .detailModel(SwingEntityModel.builder(TagItem.TYPE));
    SwingEntityModel.Builder sellerContactInfoModelBuilder =
            SwingEntityModel.builder(SellerContactInfo.TYPE)
                    .detailModel(SwingEntityModel.builder(Item.TYPE)
                            .detailModel(SwingEntityModel.builder(TagItem.TYPE)));

    return Arrays.asList(
            EntityPanel.builder(Address.TYPE)
                    .editPanel(AddressEditPanel.class),
            EntityPanel.builder(sellerContactInfoModelBuilder)
                    .editPanel(ContactInfoEditPanel.class)
                    .detailPanel(EntityPanel.builder(Item.TYPE)
                            .editPanel(ItemEditPanel.class)
                            .detailPanel(EntityPanel.builder(TagItem.TYPE)
                                    .editPanel(TagItemEditPanel.class))
                            .layout(detailPanelState(PanelState.HIDDEN))),
            EntityPanel.builder(tagModelBuilder)
                    .editPanel(TagEditPanel.class)
                    .detailPanel(EntityPanel.builder(TagItem.TYPE)
                            .editPanel(TagItemEditPanel.class))
                    .layout(detailPanelState(PanelState.HIDDEN)));
  }

  public static void main(String[] args) {
    Locale.setDefault(new Locale("en"));
    EntityPanel.TOOLBAR_CONTROLS.set(true);
    EntityApplicationPanel.builder(PetstoreAppModel.class, PetstoreAppPanel.class)
            .applicationName("The Pet Store")
            .domainType(DOMAIN)
            .defaultLoginUser(User.parse("scott:tiger"))
            .start();
  }
}