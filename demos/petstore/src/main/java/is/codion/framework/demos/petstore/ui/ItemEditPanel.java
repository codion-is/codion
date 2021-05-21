/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.*;

public class ItemEditPanel extends EntityEditPanel {

  public ItemEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Item.PRODUCT_FK);

    foreignKeyComboBox(Item.PRODUCT_FK)
            .build();
    textField(Item.NAME).columns(12)
            .build();
    textInputPanel(Item.DESCRIPTION)
            .columns(14)
            .buttonFocusable(false)
            .build();
    textField(Item.PRICE)
            .build();
    final EntityComboBox contactInfoBox = foreignKeyComboBox(Item.CONTACT_INFO_FK)
            .preferredWidth(140)
            .popupWidth(200)
            .build();
    final EntityComboBox addressBox = foreignKeyComboBox(Item.ADDRESS_FK)
            .preferredSize(TextFields.getPreferredTextFieldSize())
            .popupWidth(200)
            .build();
    textField(Item.IMAGE_URL)
            .columns(14)
            .build();
    textField(Item.IMAGE_THUMB_URL)
            .columns(14)
            .build();
    checkBox(Item.DISABLED)
            .nullable(true)
            .includeCaption(false)
            .build();

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