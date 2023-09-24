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

import javax.swing.JLabel;

import static is.codion.framework.demos.petstore.domain.Petstore.Address;

public class AddressEditPanel extends EntityEditPanel {

  public AddressEditPanel(SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Address.CITY);

    createTextField(Address.CITY);
    createTextField(Address.STATE);
    createTextField(Address.ZIP);
    createTextField(Address.STREET_1);
    createTextField(Address.STREET_2);
    createTextField(Address.LATITUDE);
    createTextField(Address.LONGITUDE);

    setLayout(Layouts.flexibleGridLayout(4, 2));
    addInputPanel(Address.CITY);
    addInputPanel(Address.STATE);
    add(new JLabel());
    addInputPanel(Address.ZIP);
    addInputPanel(Address.STREET_1);
    addInputPanel(Address.STREET_2);
    addInputPanel(Address.LATITUDE);
    addInputPanel(Address.LONGITUDE);
  }
}