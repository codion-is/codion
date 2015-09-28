/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.swing.ui.UiUtil;
import org.jminor.common.swing.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityComboBox;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.client.ui.EntityUiUtil;
import org.jminor.framework.demos.petstore.domain.Petstore;

import javax.swing.JTextField;

import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public class ItemEditPanel extends EntityEditPanel {

  public ItemEditPanel(final EntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(3,3,5,5));
    EntityComboBox box = createEntityComboBox(ITEM_PRODUCT_FK);
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
    box = createEntityComboBox(ITEM_C0NTACT_INFO_FK);
    box.setPopupWidth(200);
    box.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    add(createPropertyPanel(ITEM_C0NTACT_INFO_FK, EntityUiUtil.createEastButtonPanel(box,
            createEditPanelAction(box, new EntityPanelProvider(Petstore.T_SELLER_CONTACT_INFO).setEditPanelClass(ContactInfoEditPanel.class)), false)));
    box = createEntityComboBox(ITEM_ADDRESS_FK);
    box.setPopupWidth(200);
    box.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    add(createPropertyPanel(ITEM_ADDRESS_FK, EntityUiUtil.createEastButtonPanel(box,
            createEditPanelAction(box, new EntityPanelProvider(Petstore.T_ADDRESS).setEditPanelClass(AddressEditPanel.class)), false)));
    createTextField(ITEM_IMAGE_URL);
    addPropertyPanel(ITEM_IMAGE_URL);
    createTextField(ITEM_IMAGE_THUMB_URL);
    addPropertyPanel(ITEM_IMAGE_THUMB_URL);
    createTristateCheckBox(ITEM_DISABLED, null, false);
    addPropertyPanel(ITEM_DISABLED);
  }
}