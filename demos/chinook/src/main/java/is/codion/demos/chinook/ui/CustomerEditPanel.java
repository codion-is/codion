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
package is.codion.demos.chinook.ui;

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;

import static is.codion.demos.chinook.domain.api.Chinook.Customer;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_SPACE;

public final class CustomerEditPanel extends EntityEditPanel {

	public CustomerEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
	}

	@Override
	protected void initializeUI() {
		focus().initial().set(Customer.FIRSTNAME);

		createTextField(Customer.FIRSTNAME)
						.columns(6);
		createTextField(Customer.LASTNAME)
						.columns(6);
		createTextField(Customer.EMAIL)
						.columns(12);
		createTextField(Customer.COMPANY)
						.columns(12);
		createTextField(Customer.ADDRESS)
						.columns(12);
		createTextField(Customer.CITY)
						.columns(8);
		createTextField(Customer.POSTALCODE)
						.columns(4);
		createTextField(Customer.STATE)
						.columns(4)
						.upperCase(true)
						// CTRL-SPACE displays a dialog for selecting
						// a State from existing column values
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_SPACE)
										.modifiers(CTRL_DOWN_MASK)
										.action(Control.action(this::selectState)));
		createTextField(Customer.COUNTRY)
						.columns(8);
		createTextField(Customer.PHONE)
						.columns(12);
		createTextField(Customer.FAX)
						.columns(12);
		createComboBox(Customer.SUPPORTREP_FK)
						.preferredWidth(120);

		JPanel firstLastNamePanel = gridLayoutPanel(1, 2)
						.add(createInputPanel(Customer.FIRSTNAME))
						.add(createInputPanel(Customer.LASTNAME))
						.build();

		JPanel cityPostalCodePanel = flexibleGridLayoutPanel(1, 2)
						.add(createInputPanel(Customer.CITY))
						.add(createInputPanel(Customer.POSTALCODE))
						.build();

		JPanel stateCountryPanel = flexibleGridLayoutPanel(1, 2)
						.add(createInputPanel(Customer.STATE))
						.add(createInputPanel(Customer.COUNTRY))
						.build();

		setLayout(flexibleGridLayout(4, 3));

		add(firstLastNamePanel);
		addInputPanel(Customer.EMAIL);
		addInputPanel(Customer.COMPANY);
		addInputPanel(Customer.ADDRESS);
		add(cityPostalCodePanel);
		add(stateCountryPanel);
		addInputPanel(Customer.PHONE);
		addInputPanel(Customer.FAX);
		addInputPanel(Customer.SUPPORTREP_FK);
	}

	private void selectState(ActionEvent event) {
		JTextField stateField = (JTextField) event.getSource();
		Dialogs.select()
						.list(editModel().connection().select(Customer.STATE))
						.owner(stateField)
						.selectSingle()
						.ifPresent(stateField::setText);
	}
}