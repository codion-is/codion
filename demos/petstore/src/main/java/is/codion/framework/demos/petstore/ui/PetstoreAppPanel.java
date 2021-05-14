/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.common.model.CancelException;
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

public final class PetstoreAppPanel extends EntityApplicationPanel<PetstoreAppModel> {

  public PetstoreAppPanel() {
    super("The Pet Store");
  }

  @Override
  protected List<EntityPanel> initializeEntityPanels(final PetstoreAppModel applicationModel) {
    /* CATEGORY
     *   PRODUCT
     *     ITEM
     *       ITEMTAG
     */
    final SwingEntityModel categoryModel = applicationModel.getEntityModel(Category.TYPE);
    final SwingEntityModel productModel = categoryModel.getDetailModel(Product.TYPE);
    final SwingEntityModel itemModel = productModel.getDetailModel(Item.TYPE);
    final SwingEntityModel tagItemModel = itemModel.getDetailModel(TagItem.TYPE);

    final EntityPanel categoryPanel = new EntityPanel(categoryModel,
            new CategoryEditPanel(categoryModel.getEditModel()));
    final EntityPanel productPanel = new EntityPanel(productModel,
            new ProductEditPanel(productModel.getEditModel()));
    final EntityPanel itemPanel = new EntityPanel(itemModel,
            new ItemEditPanel(itemModel.getEditModel()));
    final EntityPanel tagItemPanel = new EntityPanel(tagItemModel,
            new TagItemEditPanel(tagItemModel.getEditModel()));

    categoryPanel.addDetailPanel(productPanel);
    categoryPanel.setDetailSplitPanelResizeWeight(0.3);
    productPanel.addDetailPanel(itemPanel);
    itemPanel.addDetailPanels(tagItemPanel);
    itemPanel.setDetailPanelState(EntityPanel.PanelState.HIDDEN);

    categoryModel.refresh();

    return Collections.singletonList(categoryPanel);
  }

  @Override
  protected List<EntityPanel.Builder> initializeSupportEntityPanelBuilders(final PetstoreAppModel applicationModel) {
    final SwingEntityModel.Builder tagModelBuilder =
            SwingEntityModel.builder(Tag.TYPE)
                    .detailModelBuilder(SwingEntityModel.builder(TagItem.TYPE));
    final SwingEntityModel.Builder sellerContactInfoModelBuilder =
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

  @Override
  protected PetstoreAppModel initializeApplicationModel(final EntityConnectionProvider connectionProvider)
          throws CancelException {
    return new PetstoreAppModel(connectionProvider);
  }

  public static void main(final String[] args) {
    Locale.setDefault(new Locale("en"));
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.petstore.domain.Petstore");
    SwingUtilities.invokeLater(() -> new PetstoreAppPanel().starter()
            .frameSize(Windows.getScreenSizeRatio(0.8))
            .defaultLoginUser(User.parseUser("scott:tiger"))
            .start());
  }
}