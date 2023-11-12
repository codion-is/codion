/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.component.EntityComboBox;

import static is.codion.framework.demos.petstore.domain.Petstore.*;
import static is.codion.swing.common.ui.component.button.ButtonPanelBuilder.createEastButtonPanel;

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
    EntityComboBox contactInfoBox = createForeignKeyComboBox(Item.CONTACT_INFO_FK)
            .preferredWidth(140)
            .build();
    EntityComboBox addressBox = createForeignKeyComboBox(Item.ADDRESS_FK)
            .preferredWidth(140)
            .build();
    createTextField(Item.IMAGE_URL);
    createTextField(Item.IMAGE_THUMB_URL);
    createCheckBox(Item.DISABLED);

    setLayout(Layouts.flexibleGridLayout(3, 3));
    addInputPanel(Item.PRODUCT_FK);
    addInputPanel(Item.NAME);
    addInputPanel(Item.DESCRIPTION);
    addInputPanel(Item.PRICE);
    addInputPanel(Item.CONTACT_INFO_FK, createEastButtonPanel(contactInfoBox,
            createAddControl(contactInfoBox, () ->
                    new ContactInfoEditPanel(new SwingEntityEditModel(SellerContactInfo.TYPE, editModel().connectionProvider())))));
    addInputPanel(Item.ADDRESS_FK, createEastButtonPanel(addressBox,
            createAddControl(addressBox, () ->
                    new AddressEditPanel(new SwingEntityEditModel(Address.TYPE, editModel().connectionProvider())))));
    addInputPanel(Item.IMAGE_URL);
    addInputPanel(Item.IMAGE_THUMB_URL);
    addInputPanel(Item.DISABLED);
  }
}