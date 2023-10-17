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

import static is.codion.framework.demos.petstore.domain.Petstore.SellerContactInfo;

public class ContactInfoEditPanel extends EntityEditPanel {

  public ContactInfoEditPanel(SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(SellerContactInfo.LAST_NAME);

    createTextField(SellerContactInfo.LAST_NAME);
    createTextField(SellerContactInfo.FIRST_NAME);
    createTextField(SellerContactInfo.EMAIL);

    setLayout(Layouts.flexibleGridLayout(3, 1));
    addInputPanel(SellerContactInfo.LAST_NAME);
    addInputPanel(SellerContactInfo.FIRST_NAME);
    addInputPanel(SellerContactInfo.EMAIL);
  }
}