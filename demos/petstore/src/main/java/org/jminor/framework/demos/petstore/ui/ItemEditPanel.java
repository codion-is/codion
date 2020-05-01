/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.ui;

import org.jminor.swing.common.ui.Components;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.common.ui.textfield.TextFields;
import org.jminor.swing.common.ui.textfield.TextInputPanel;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelBuilder;

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
    Components.setPreferredWidth(contactInfoBox, 140);
    contactInfoBox.setPopupWidth(200);
    final EntityComboBox addressBox = createForeignKeyComboBox(ITEM_ADDRESS_FK);
    Components.setPreferredWidth(addressBox, 140);
    addressBox.setPopupWidth(200);
    addressBox.setPreferredSize(TextFields.getPreferredTextFieldSize());
    createTextField(ITEM_IMAGE_URL).setColumns(14);
    createTextField(ITEM_IMAGE_THUMB_URL).setColumns(14);
    createNullableCheckBox(ITEM_DISABLED, null, false);

    setLayout(new FlexibleGridLayout(3, 3, 5, 5));
    addPropertyPanel(ITEM_PRODUCT_FK);
    addPropertyPanel(ITEM_NAME);
    add(createPropertyPanel(ITEM_DESCRIPTION));
    addPropertyPanel(ITEM_PRICE);
    add(createPropertyPanel(ITEM_C0NTACT_INFO_FK, Components.createEastButtonPanel(contactInfoBox,
            createEditPanelAction(contactInfoBox, new EntityPanelBuilder(T_SELLER_CONTACT_INFO)
                    .setEditPanelClass(ContactInfoEditPanel.class)))));
    add(createPropertyPanel(ITEM_ADDRESS_FK, Components.createEastButtonPanel(addressBox,
            createEditPanelAction(addressBox, new EntityPanelBuilder(T_ADDRESS)
                    .setEditPanelClass(AddressEditPanel.class)))));
    addPropertyPanel(ITEM_IMAGE_URL);
    addPropertyPanel(ITEM_IMAGE_THUMB_URL);
    addPropertyPanel(ITEM_DISABLED);
  }
}