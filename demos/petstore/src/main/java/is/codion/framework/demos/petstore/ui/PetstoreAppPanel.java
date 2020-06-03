/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.common.model.CancelException;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petstore.model.PetstoreAppModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;

import java.util.Locale;

import static is.codion.framework.demos.petstore.domain.Petstore.*;

public final class PetstoreAppPanel extends EntityApplicationPanel<PetstoreAppModel> {

  @Override
  protected void setupEntityPanelBuilders() {
    /* CATEGORY
     *   PRODUCT
     *     ITEM
     *       ITEMTAG
     */
    final EntityPanelBuilder tagItemProvider = new EntityPanelBuilder(TagItem.TYPE)
            .setEditPanelClass(TagItemEditPanel.class);
    final EntityPanelBuilder itemProvider = new EntityPanelBuilder(Item.TYPE)
            .setEditPanelClass(ItemEditPanel.class);
    itemProvider.addDetailPanelBuilder(tagItemProvider).setDetailPanelState(EntityPanel.PanelState.HIDDEN);
    final EntityPanelBuilder productProvider = new EntityPanelBuilder(Product.TYPE)
            .setEditPanelClass(ProductEditPanel.class);
    productProvider.addDetailPanelBuilder(itemProvider).setDetailSplitPanelResizeWeight(0.3);
    final EntityPanelBuilder categoryProvider = new EntityPanelBuilder(Category.TYPE)
            .setEditPanelClass(CategoryEditPanel.class);
    categoryProvider.addDetailPanelBuilder(productProvider).setDetailSplitPanelResizeWeight(0.3);

    addEntityPanelBuilder(categoryProvider);

    final EntityPanelBuilder addressProvider = new EntityPanelBuilder(Address.TYPE)
            .setEditPanelClass(AddressEditPanel.class);
    final EntityPanelBuilder contactInfoProvider = new EntityPanelBuilder(SellerContactInfo.TYPE)
            .setEditPanelClass(ContactInfoEditPanel.class);
    contactInfoProvider.addDetailPanelBuilder(itemProvider);
    final EntityPanelBuilder tagProvider = new EntityPanelBuilder(Tag.TYPE)
            .setEditPanelClass(TagEditPanel.class);
    tagProvider.addDetailPanelBuilder(tagItemProvider).setDetailPanelState(EntityPanel.PanelState.HIDDEN);

    addSupportPanelBuilders(addressProvider, contactInfoProvider, tagProvider);
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
    new PetstoreAppPanel().startApplication("The Pet Store", null, MaximizeFrame.NO,
            Windows.getScreenSizeRatio(0.8), Users.parseUser("scott:tiger"));
  }
}