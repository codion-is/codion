/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.swing.common.ui.TextInputPanel;
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
    final TextInputPanel descriptionPanel = createTextInputPanel(ITEM_DESCRIPTION);
    descriptionPanel.getTextField().setColumns(14);
    descriptionPanel.getButton().setFocusable(false);
    createTextField(ITEM_PRICE);
    final EntityComboBox contactInfoBox = createForeignKeyComboBox(ITEM_C0NTACT_INFO_FK);
    UiUtil.setPreferredWidth(contactInfoBox, 140);
    contactInfoBox.setPopupWidth(200);
    final EntityComboBox addressBox = createForeignKeyComboBox(ITEM_ADDRESS_FK);
    UiUtil.setPreferredWidth(addressBox, 140);
    addressBox.setPopupWidth(200);
    addressBox.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    createTextField(ITEM_IMAGE_URL).setColumns(14);
    createTextField(ITEM_IMAGE_THUMB_URL).setColumns(14);
    createTristateCheckBox(ITEM_DISABLED, null, false);

    setLayout(new FlexibleGridLayout(3, 3, 5, 5));
    addPropertyPanel(ITEM_PRODUCT_FK);
    addPropertyPanel(ITEM_NAME);
    add(createPropertyPanel(ITEM_DESCRIPTION, descriptionPanel));
    addPropertyPanel(ITEM_PRICE);
    add(createPropertyPanel(ITEM_C0NTACT_INFO_FK, UiUtil.createEastButtonPanel(contactInfoBox,
            createEditPanelAction(contactInfoBox, new EntityPanelProvider(Petstore.T_SELLER_CONTACT_INFO)
                    .setEditPanelClass(ContactInfoEditPanel.class)), false)));
    add(createPropertyPanel(ITEM_ADDRESS_FK, UiUtil.createEastButtonPanel(addressBox,
            createEditPanelAction(addressBox, new EntityPanelProvider(Petstore.T_ADDRESS)
                    .setEditPanelClass(AddressEditPanel.class)), false)));
    addPropertyPanel(ITEM_IMAGE_URL);
    addPropertyPanel(ITEM_IMAGE_THUMB_URL);
    addPropertyPanel(ITEM_DISABLED);
  }
}