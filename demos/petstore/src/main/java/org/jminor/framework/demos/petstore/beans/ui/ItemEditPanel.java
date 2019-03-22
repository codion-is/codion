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

import javax.swing.JTextField;

import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public class ItemEditPanel extends EntityEditPanel {

  public ItemEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(3, 3, 5, 5));
    EntityComboBox box = createForeignKeyComboBox(ITEM_PRODUCT_FK);
    setInitialFocusComponent(box);
    addPropertyPanel(ITEM_PRODUCT_FK);
    JTextField txt = createTextField(ITEM_NAME);
    txt.setColumns(12);
    addPropertyPanel(ITEM_NAME);
    txt = createTextField(ITEM_DESCRIPTION);
    txt.setColumns(16);
    addPropertyPanel(ITEM_DESCRIPTION);
    createTextField(ITEM_PRICE);
    addPropertyPanel(ITEM_PRICE);
    box = createForeignKeyComboBox(ITEM_C0NTACT_INFO_FK);
    box.setPopupWidth(200);
    box.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    add(createPropertyPanel(ITEM_C0NTACT_INFO_FK, UiUtil.createEastButtonPanel(box,
            createEditPanelAction(box, new EntityPanelProvider(Petstore.T_SELLER_CONTACT_INFO,
                    getEditModel().getDomain().getCaption(Petstore.T_SELLER_CONTACT_INFO)).setEditPanelClass(ContactInfoEditPanel.class)), false)));
    box = createForeignKeyComboBox(ITEM_ADDRESS_FK);
    box.setPopupWidth(200);
    box.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    add(createPropertyPanel(ITEM_ADDRESS_FK, UiUtil.createEastButtonPanel(box,
            createEditPanelAction(box, new EntityPanelProvider(Petstore.T_ADDRESS,
                    getEditModel().getDomain().getCaption(Petstore.T_ADDRESS)).setEditPanelClass(AddressEditPanel.class)), false)));
    createTextField(ITEM_IMAGE_URL);
    addPropertyPanel(ITEM_IMAGE_URL);
    createTextField(ITEM_IMAGE_THUMB_URL);
    addPropertyPanel(ITEM_IMAGE_THUMB_URL);
    createTristateCheckBox(ITEM_DISABLED, null, false);
    addPropertyPanel(ITEM_DISABLED);
  }
}