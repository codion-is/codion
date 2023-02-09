/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petstore.model.PetstoreAppModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static is.codion.framework.demos.petstore.domain.Petstore.*;
import static is.codion.swing.framework.ui.EntityApplicationBuilder.entityApplicationBuilder;

public final class PetstoreAppPanel extends EntityApplicationPanel<PetstoreAppModel> {

  public PetstoreAppPanel(PetstoreAppModel applicationModel) {
    super(applicationModel);
  }

  @Override
  protected List<EntityPanel> createEntityPanels(PetstoreAppModel applicationModel) {
    /* CATEGORY
     *   PRODUCT
     *     ITEM
     *       ITEMTAG
     */
    SwingEntityModel categoryModel = applicationModel.entityModel(Category.TYPE);
    SwingEntityModel productModel = categoryModel.detailModel(Product.TYPE);
    SwingEntityModel itemModel = productModel.detailModel(Item.TYPE);
    SwingEntityModel tagItemModel = itemModel.detailModel(TagItem.TYPE);

    EntityPanel categoryPanel = new EntityPanel(categoryModel,
            new CategoryEditPanel(categoryModel.editModel()));
    EntityPanel productPanel = new EntityPanel(productModel,
            new ProductEditPanel(productModel.editModel()));
    EntityPanel itemPanel = new EntityPanel(itemModel,
            new ItemEditPanel(itemModel.editModel()));
    EntityPanel tagItemPanel = new EntityPanel(tagItemModel,
            new TagItemEditPanel(tagItemModel.editModel()));

    categoryPanel.addDetailPanel(productPanel);
    categoryPanel.setDetailSplitPanelResizeWeight(0.3);
    productPanel.addDetailPanel(itemPanel);
    itemPanel.addDetailPanels(tagItemPanel);
    itemPanel.setDetailPanelState(EntityPanel.PanelState.HIDDEN);

    return Collections.singletonList(categoryPanel);
  }

  @Override
  protected List<EntityPanel.Builder> createSupportEntityPanelBuilders(PetstoreAppModel applicationModel) {
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
                            .detailPanelState(EntityPanel.PanelState.HIDDEN)),
            EntityPanel.builder(tagModelBuilder)
                    .editPanelClass(TagEditPanel.class)
                    .detailPanelBuilder(EntityPanel.builder(TagItem.TYPE)
                            .editPanelClass(TagItemEditPanel.class))
                    .detailPanelState(EntityPanel.PanelState.HIDDEN));
  }

  public static void main(String[] args) {
    Locale.setDefault(new Locale("en"));
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.petstore.domain.Petstore");
    SwingUtilities.invokeLater(() -> entityApplicationBuilder(PetstoreAppModel.class, PetstoreAppPanel.class)
            .applicationName("The Pet Store")
            .frameSize(Windows.screenSizeRatio(0.8))
            .defaultLoginUser(User.parse("scott:tiger"))
            .start());
  }
}