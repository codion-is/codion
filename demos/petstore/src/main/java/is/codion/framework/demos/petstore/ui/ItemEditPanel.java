/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.*;

public class ItemEditPanel extends EntityEditPanel {

  public ItemEditPanel(final SwingEntityEditModel model) {
    super(model);
    setDefaultTextFieldColumns(14);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Item.PRODUCT_FK);

    createForeignKeyComboBox(Item.PRODUCT_FK);
    createTextField(Item.NAME);
    createTextInputPanel(Item.DESCRIPTION)
            .buttonFocusable(false);
    createTextField(Item.PRICE);
    final EntityComboBox contactInfoBox = createForeignKeyComboBox(Item.CONTACT_INFO_FK)
            .preferredWidth(140)
            .popupWidth(200)
            .build();
    final EntityComboBox addressBox = createForeignKeyComboBox(Item.ADDRESS_FK)
            .popupWidth(200)
            .build();
    createTextField(Item.IMAGE_URL);
    createTextField(Item.IMAGE_THUMB_URL);
    createBooleanComboBox(Item.DISABLED);

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