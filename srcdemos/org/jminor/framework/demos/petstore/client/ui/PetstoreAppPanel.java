/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.petstore.beans.ui.AddressPanel;
import org.jminor.framework.demos.petstore.beans.ui.CategoryPanel;
import org.jminor.framework.demos.petstore.beans.ui.ContactInfoPanel;
import org.jminor.framework.demos.petstore.beans.ui.ItemPanel;
import org.jminor.framework.demos.petstore.beans.ui.ProductPanel;
import org.jminor.framework.demos.petstore.beans.ui.TagItemPanel;
import org.jminor.framework.demos.petstore.beans.ui.TagPanel;
import org.jminor.framework.demos.petstore.domain.Petstore;

import java.util.Locale;

public class PetstoreAppPanel extends EntityApplicationPanel {

  public PetstoreAppPanel() {
    final EntityPanelProvider tagItemProvider = new EntityPanelProvider(Petstore.T_TAG_ITEM).setEditPanelClass(TagItemPanel.class);
    final EntityPanelProvider itemProvider = new EntityPanelProvider(Petstore.T_ITEM).setEditPanelClass(ItemPanel.class);
    itemProvider.addDetailPanelProvider(tagItemProvider).setDetailPanelState(EntityPanel.HIDDEN);
    final EntityPanelProvider productProvider = new EntityPanelProvider(Petstore.T_PRODUCT).setEditPanelClass(ProductPanel.class);
    productProvider.addDetailPanelProvider(itemProvider).setDetailSplitPanelResizeWeight(0.3);
    final EntityPanelProvider categoryProvider = new EntityPanelProvider(Petstore.T_CATEGORY).setEditPanelClass(CategoryPanel.class);
    categoryProvider.addDetailPanelProvider(productProvider).setDetailSplitPanelResizeWeight(0.3);

    addMainApplicationPanelProvider(categoryProvider);

    final EntityPanelProvider addressProvider = new EntityPanelProvider(Petstore.T_ADDRESS).setEditPanelClass(AddressPanel.class);
    final EntityPanelProvider contactInfoProvider = new EntityPanelProvider(Petstore.T_SELLER_CONTACT_INFO).setEditPanelClass(ContactInfoPanel.class);
    contactInfoProvider.addDetailPanelProvider(itemProvider);
    final EntityPanelProvider tagProvider = new EntityPanelProvider(Petstore.T_TAG).setEditPanelClass(TagPanel.class);
    tagProvider.addDetailPanelProvider(tagItemProvider).setDetailPanelState(EntityPanel.HIDDEN);

    addSupportPanelProviders(addressProvider, contactInfoProvider, tagProvider);
  }

  @Override
  protected void configureApplication() {
    Locale.setDefault(new Locale("en"));
    Configuration.setValue(Configuration.TOOLBAR_BUTTONS, true);
    Configuration.setValue(Configuration.PROPERTY_DEBUG_OUTPUT, true);
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel(final EntityDbProvider dbProvider) throws CancelException {
    return new EntityApplicationModel(dbProvider) {
      @Override
      protected void loadDomainModel() {
        new Petstore();
      }
    };
  }

  public static void main(final String[] args) {
    new PetstoreAppPanel().startApplication("The Pet Store", null, false, UiUtil.getScreenSizeRatio(0.8), new User("scott", "tiger"));
  }
}