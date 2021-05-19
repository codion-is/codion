/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityInputComponents.IncludeCaption;
import is.codion.swing.framework.ui.EntityPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.*;

public class ItemEditPanel extends EntityEditPanel {

  public ItemEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Item.PRODUCT_FK);

    createForeignKeyComboBox(Item.PRODUCT_FK);
    createTextField(Item.NAME).setColumns(12);
    final TextInputPanel descriptionPanel = createTextInputPanel(Item.DESCRIPTION);
    descriptionPanel.getTextField().setColumns(14);
    descriptionPanel.getButton().setFocusable(false);
    createTextField(Item.PRICE);
    final EntityComboBox contactInfoBox = createForeignKeyComboBox(Item.CONTACT_INFO_FK);
    Components.setPreferredWidth(contactInfoBox, 140);
    contactInfoBox.setPopupWidth(200);
    final EntityComboBox addressBox = createForeignKeyComboBox(Item.ADDRESS_FK);
    Components.setPreferredWidth(addressBox, 140);
    addressBox.setPopupWidth(200);
    addressBox.setPreferredSize(TextFields.getPreferredTextFieldSize());
    createTextField(Item.IMAGE_URL).setColumns(14);
    createTextField(Item.IMAGE_THUMB_URL).setColumns(14);
    createNullableCheckBox(Item.DISABLED, IncludeCaption.NO);

    setLayout(Layouts.flexibleGridLayout(3, 3));
    addInputPanel(Item.PRODUCT_FK);
    addInputPanel(Item.NAME);
    addInputPanel(Item.DESCRIPTION);
    addInputPanel(Item.PRICE);
    addInputPanel(Item.CONTACT_INFO_FK, Components.createEastButtonPanel(contactInfoBox,
            EntityPanel.builder(SellerContactInfo.TYPE)
                    .editPanelClass(ContactInfoEditPanel.class)
                    .createEditPanelAction(contactInfoBox)));
    addInputPanel(Item.ADDRESS_FK, Components.createEastButtonPanel(addressBox,
            EntityPanel.builder(Address.TYPE)
                    .editPanelClass(AddressEditPanel.class)
                    .createEditPanelAction(addressBox)));
    addInputPanel(Item.IMAGE_URL);
    addInputPanel(Item.IMAGE_THUMB_URL);
    addInputPanel(Item.DISABLED);
  }
}