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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

// tag::addressEditPanel[]
public class AddressEditPanel extends EntityEditPanel {

	public AddressEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
	}

	@Override
	protected void initializeUI() {
		initialFocusAttribute().set(Address.STREET);

		createTextField(Address.STREET).columns(25);
		createTextField(Address.CITY).columns(25);
		createCheckBox(Address.VALID);

		setLayout(new GridLayout(3, 1, 5, 5));
		addInputPanel(Address.STREET);
		addInputPanel(Address.CITY);
		addInputPanel(Address.VALID);
	}
}
// end::addressEditPanel[]