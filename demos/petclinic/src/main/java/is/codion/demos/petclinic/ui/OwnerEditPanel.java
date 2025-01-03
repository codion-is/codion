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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.petclinic.ui;

import is.codion.demos.petclinic.domain.api.Owner;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class OwnerEditPanel extends EntityEditPanel {

	public OwnerEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
	}

	@Override
	protected void initializeUI() {
		focus().initial().set(Owner.FIRST_NAME);

		createTextField(Owner.FIRST_NAME);
		createTextField(Owner.LAST_NAME);
		createTextField(Owner.ADDRESS);
		createTextField(Owner.CITY);
		createTextField(Owner.TELEPHONE);
		createComboBox(Owner.PHONE_TYPE);

		setLayout(gridLayout(3, 2));

		addInputPanel(Owner.FIRST_NAME);
		addInputPanel(Owner.LAST_NAME);
		addInputPanel(Owner.ADDRESS);
		addInputPanel(Owner.CITY);
		addInputPanel(Owner.TELEPHONE);
		addInputPanel(Owner.PHONE_TYPE);
	}
}
