/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.petstore.ui;

import dev.codion.common.model.CancelException;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.demos.petstore.model.PetstoreAppModel;
import dev.codion.swing.common.ui.Windows;
import dev.codion.swing.framework.ui.EntityApplicationPanel;
import dev.codion.swing.framework.ui.EntityPanel;
import dev.codion.swing.framework.ui.EntityPanelBuilder;

import java.util.Locale;

import static dev.codion.framework.demos.petstore.domain.Petstore.*;

public final class PetstoreAppPanel extends EntityApplicationPanel<PetstoreAppModel> {

  @Override
  protected void setupEntityPanelBuilders() {
    /* CATEGORY
     *   PRODUCT
     *     ITEM
     *       ITEMTAG
     */
    final EntityPanelBuilder tagItemProvider = new EntityPanelBuilder(T_TAG_ITEM)
            .setEditPanelClass(TagItemEditPanel.class);
    final EntityPanelBuilder itemProvider = new EntityPanelBuilder(T_ITEM)
            .setEditPanelClass(ItemEditPanel.class);
    itemProvider.addDetailPanelBuilder(tagItemProvider).setDetailPanelState(EntityPanel.PanelState.HIDDEN);
    final EntityPanelBuilder productProvider = new EntityPanelBuilder(T_PRODUCT)
            .setEditPanelClass(ProductEditPanel.class);
    productProvider.addDetailPanelBuilder(itemProvider).setDetailSplitPanelResizeWeight(0.3);
    final EntityPanelBuilder categoryProvider = new EntityPanelBuilder(T_CATEGORY)
            .setEditPanelClass(CategoryEditPanel.class);
    categoryProvider.addDetailPanelBuilder(productProvider).setDetailSplitPanelResizeWeight(0.3);

    addEntityPanelBuilder(categoryProvider);

    final EntityPanelBuilder addressProvider = new EntityPanelBuilder(T_ADDRESS)
            .setEditPanelClass(AddressEditPanel.class);
    final EntityPanelBuilder contactInfoProvider = new EntityPanelBuilder(T_SELLER_CONTACT_INFO)
            .setEditPanelClass(ContactInfoEditPanel.class);
    contactInfoProvider.addDetailPanelBuilder(itemProvider);
    final EntityPanelBuilder tagProvider = new EntityPanelBuilder(T_TAG)
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
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("org.jminor.framework.demos.petstore.domain.Petstore");
    new PetstoreAppPanel().startApplication("The Pet Store", null, MaximizeFrame.NO,
            Windows.getScreenSizeRatio(0.8), Users.parseUser("scott:tiger"));
  }
}