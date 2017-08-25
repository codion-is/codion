/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.client.ui;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.petstore.beans.ui.AddressEditPanel;
import org.jminor.framework.demos.petstore.beans.ui.CategoryEditPanel;
import org.jminor.framework.demos.petstore.beans.ui.ContactInfoEditPanel;
import org.jminor.framework.demos.petstore.beans.ui.ItemEditPanel;
import org.jminor.framework.demos.petstore.beans.ui.ProductEditPanel;
import org.jminor.framework.demos.petstore.beans.ui.TagEditPanel;
import org.jminor.framework.demos.petstore.beans.ui.TagItemEditPanel;
import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.framework.domain.Entities;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;

import java.util.Locale;

public final class PetstoreAppPanel extends EntityApplicationPanel<PetstoreAppPanel.PetstoreApplicationModel> {

  @Override
  protected Entities initializeEntities() {
    return new Petstore();
  }

  @Override
  protected void setupEntityPanelProviders() {
   /* CATEGORY
    *   PRODUCT
    *     ITEM
    *       ITEMTAG
    */
    final Entities entities = getModel().getEntities();
    final EntityPanelProvider tagItemProvider = new EntityPanelProvider(Petstore.T_TAG_ITEM,
            entities.getCaption(Petstore.T_TAG_ITEM)).setEditPanelClass(TagItemEditPanel.class);
    final EntityPanelProvider itemProvider = new EntityPanelProvider(Petstore.T_ITEM,
            entities.getCaption(Petstore.T_ITEM)).setEditPanelClass(ItemEditPanel.class);
    itemProvider.addDetailPanelProvider(tagItemProvider).setDetailPanelState(EntityPanel.PanelState.HIDDEN);
    final EntityPanelProvider productProvider = new EntityPanelProvider(Petstore.T_PRODUCT,
            entities.getCaption(Petstore.T_PRODUCT)).setEditPanelClass(ProductEditPanel.class);
    productProvider.addDetailPanelProvider(itemProvider).setDetailSplitPanelResizeWeight(0.3);
    final EntityPanelProvider categoryProvider = new EntityPanelProvider(Petstore.T_CATEGORY,
            entities.getCaption(Petstore.T_CATEGORY)).setEditPanelClass(CategoryEditPanel.class);
    categoryProvider.addDetailPanelProvider(productProvider).setDetailSplitPanelResizeWeight(0.3);

    addEntityPanelProvider(categoryProvider);

    final EntityPanelProvider addressProvider = new EntityPanelProvider(Petstore.T_ADDRESS,
            entities.getCaption(Petstore.T_ADDRESS)).setEditPanelClass(AddressEditPanel.class);
    final EntityPanelProvider contactInfoProvider = new EntityPanelProvider(Petstore.T_SELLER_CONTACT_INFO,
            entities.getCaption(Petstore.T_SELLER_CONTACT_INFO)).setEditPanelClass(ContactInfoEditPanel.class);
    contactInfoProvider.addDetailPanelProvider(itemProvider);
    final EntityPanelProvider tagProvider = new EntityPanelProvider(Petstore.T_TAG,
            entities.getCaption(Petstore.T_TAG)).setEditPanelClass(TagEditPanel.class);
    tagProvider.addDetailPanelProvider(tagItemProvider).setDetailPanelState(EntityPanel.PanelState.HIDDEN);

    addSupportPanelProviders(addressProvider, contactInfoProvider, tagProvider);
  }

  @Override
  protected PetstoreApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) throws CancelException {
    return new PetstoreApplicationModel(connectionProvider);
  }

  public static void main(final String[] args) {
    Locale.setDefault(new Locale("en"));
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    new PetstoreAppPanel().startApplication("The Pet Store", null, false, UiUtil.getScreenSizeRatio(0.8), new User("scott", "tiger"));
  }

  public static final class PetstoreApplicationModel extends SwingEntityApplicationModel {
    public PetstoreApplicationModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
    }

  }
}