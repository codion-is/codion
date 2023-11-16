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

import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.framework.demos.manual.store.domain.Store.CustomerAddress;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

// tag::customerAddressEditPanel[]
public class CustomerAddressEditPanel extends EntityEditPanel {

  public CustomerAddressEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(CustomerAddress.ADDRESS_FK);

    createForeignKeyComboBoxPanel(CustomerAddress.ADDRESS_FK, this::createAddressEditPanel)
            .preferredWidth(280)
            .add(true);

    setLayout(borderLayout());

    addInputPanel(CustomerAddress.ADDRESS_FK);
  }

  private AddressEditPanel createAddressEditPanel() {
    return new AddressEditPanel(new SwingEntityEditModel(Address.TYPE, editModel().connectionProvider()));
  }
}
// end::customerAddressEditPanel[]