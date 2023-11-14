/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.*;

public class ItemEditPanel extends EntityEditPanel {

  public ItemEditPanel(SwingEntityEditModel model) {
    super(model);
    setDefaultTextFieldColumns(14);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Item.PRODUCT_FK);

    createForeignKeyComboBox(Item.PRODUCT_FK);
    createTextField(Item.NAME);
    createTextFieldPanel(Item.DESCRIPTION)
            .buttonFocusable(false);
    createTextField(Item.PRICE);
    createForeignKeyComboBoxPanel(Item.CONTACT_INFO_FK, () ->
            new ContactInfoEditPanel(new SwingEntityEditModel(SellerContactInfo.TYPE, editModel().connectionProvider())))
            .preferredWidth(220)
            .addButton(true);
    createForeignKeyComboBoxPanel(Item.ADDRESS_FK, () ->
            new AddressEditPanel(new SwingEntityEditModel(Address.TYPE, editModel().connectionProvider())))
            .preferredWidth(220)
            .addButton(true);
    createTextField(Item.IMAGE_URL);
    createTextField(Item.IMAGE_THUMB_URL);
    createCheckBox(Item.DISABLED);

    setLayout(Layouts.flexibleGridLayout(3, 3));
    addInputPanel(Item.PRODUCT_FK);
    addInputPanel(Item.NAME);
    addInputPanel(Item.DESCRIPTION);
    addInputPanel(Item.PRICE);
    addInputPanel(Item.CONTACT_INFO_FK);
    addInputPanel(Item.ADDRESS_FK);
    addInputPanel(Item.IMAGE_URL);
    addInputPanel(Item.IMAGE_THUMB_URL);
    addInputPanel(Item.DISABLED);
  }
}