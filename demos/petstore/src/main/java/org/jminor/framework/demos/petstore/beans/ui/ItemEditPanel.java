/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;

import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public class ItemEditPanel extends EntityEditPanel {

  public ItemEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(ITEM_PRODUCT_FK);

    createForeignKeyComboBox(ITEM_PRODUCT_FK);
    createTextField(ITEM_NAME).setColumns(12);
    createTextField(ITEM_DESCRIPTION).setColumns(16);
    createTextField(ITEM_PRICE);
    final EntityComboBox contactInfoBox = createForeignKeyComboBox(ITEM_C0NTACT_INFO_FK);
    contactInfoBox.setPopupWidth(200);
    contactInfoBox.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    final EntityComboBox addressBox = createForeignKeyComboBox(ITEM_ADDRESS_FK);
    addressBox.setPopupWidth(200);
    addressBox.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    createTextField(ITEM_IMAGE_URL);
    createTextField(ITEM_IMAGE_THUMB_URL);
    createTristateCheckBox(ITEM_DISABLED, null, false);

    setLayout(new FlexibleGridLayout(3, 3, 5, 5));
    addPropertyPanel(ITEM_PRODUCT_FK);
    addPropertyPanel(ITEM_NAME);
    addPropertyPanel(ITEM_DESCRIPTION);
    addPropertyPanel(ITEM_PRICE);
    add(createPropertyPanel(ITEM_C0NTACT_INFO_FK, UiUtil.createEastButtonPanel(contactInfoBox,
            createEditPanelAction(contactInfoBox, new EntityPanelProvider(Petstore.T_SELLER_CONTACT_INFO,
                    getEditModel().getDomain().getCaption(Petstore.T_SELLER_CONTACT_INFO)).setEditPanelClass(ContactInfoEditPanel.class)), false)));
    add(createPropertyPanel(ITEM_ADDRESS_FK, UiUtil.createEastButtonPanel(addressBox,
            createEditPanelAction(addressBox, new EntityPanelProvider(Petstore.T_ADDRESS,
                    getEditModel().getDomain().getCaption(Petstore.T_ADDRESS)).setEditPanelClass(AddressEditPanel.class)), false)));
    addPropertyPanel(ITEM_IMAGE_URL);
    addPropertyPanel(ITEM_IMAGE_THUMB_URL);
    addPropertyPanel(ITEM_DISABLED);
  }
}