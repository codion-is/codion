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
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

// tag::customerEditPanel[]
public class CustomerEditPanel extends EntityEditPanel {

  public CustomerEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    setDefaultTextFieldColumns(15);
  }

  @Override
  protected void initializeUI() {
    //the firstName field should receive the focus whenever the panel is initialized
    setInitialFocusAttribute(Customer.FIRST_NAME);

    createTextField(Customer.FIRST_NAME);
    createTextField(Customer.LAST_NAME);
    createTextField(Customer.EMAIL);
    createCheckBox(Customer.ACTIVE);

    setLayout(new GridLayout(4, 1));
    //the addInputPanel method creates and adds a panel containing the
    //component associated with the attribute as well as a JLabel with the
    //property caption as defined in the domain model
    addInputPanel(Customer.FIRST_NAME);
    addInputPanel(Customer.LAST_NAME);
    addInputPanel(Customer.EMAIL);
    addInputPanel(Customer.ACTIVE);
  }
}
// end::customerEditPanel[]