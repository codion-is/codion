/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petstore.model.PetstoreAppModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;

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
    final EntityPanel.Builder tagItemProvider = EntityPanel.builder(TagItem.TYPE)
            .editPanelClass(TagItemEditPanel.class);

    addEntityPanelBuilder(EntityPanel.builder(Category.TYPE)
            .editPanelClass(CategoryEditPanel.class)
            .detailPanelBuilder(EntityPanel.builder(Product.TYPE)
                    .editPanelClass(ProductEditPanel.class)
                    .detailPanelBuilder(EntityPanel.builder(Item.TYPE)
                            .editPanelClass(ItemEditPanel.class)
                            .detailPanelBuilder(tagItemProvider)
                            .detailPanelState(EntityPanel.PanelState.HIDDEN))
                    .detailSplitPanelResizeWeight(0.3)).detailSplitPanelResizeWeight(0.3));

    addSupportPanelBuilders(
            EntityPanel.builder(Address.TYPE)
                    .editPanelClass(AddressEditPanel.class),
            EntityPanel.builder(SellerContactInfo.TYPE)
                    .editPanelClass(ContactInfoEditPanel.class)
                    .detailPanelBuilder(EntityPanel.builder(Item.TYPE)
                            .editPanelClass(ItemEditPanel.class)
                            .detailPanelBuilder(tagItemProvider)
                            .detailPanelState(EntityPanel.PanelState.HIDDEN)),
            EntityPanel.builder(Tag.TYPE)
                    .editPanelClass(TagEditPanel.class)
                    .detailPanelBuilder(tagItemProvider)
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
    new PetstoreAppPanel().startApplication("The Pet Store", null, MaximizeFrame.NO,
            Windows.getScreenSizeRatio(0.8), User.parseUser("scott:tiger"));
  }
}