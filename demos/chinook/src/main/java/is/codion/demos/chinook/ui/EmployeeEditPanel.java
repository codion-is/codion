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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;

import static is.codion.demos.chinook.domain.api.Chinook.Employee;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.key.TransferFocusOnEnter.BACKWARD;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;

public final class EmployeeEditPanel extends EntityEditPanel {

	public EmployeeEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
	}

	@Override
	protected void initializeUI() {
		create().textField(Employee.FIRSTNAME)
						.columns(6);
		create().textField(Employee.LASTNAME)
						.columns(6);
		create().temporalFieldPanel(Employee.BIRTHDATE)
						.columns(7);
		create().temporalFieldPanel(Employee.HIREDATE)
						.columns(7);
		create().textField(Employee.TITLE)
						.columns(8);
		create().textField(Employee.ADDRESS);
		create().textField(Employee.CITY)
						.columns(8);
		create().textField(Employee.POSTALCODE)
						.columns(4);
		create().textField(Employee.STATE)
						.columns(4)
						.upperCase(true);
		create().textField(Employee.COUNTRY)
						.columns(8);
		create().textField(Employee.PHONE)
						.columns(12);
		create().textField(Employee.FAX)
						.columns(12);
		create().textField(Employee.EMAIL)
						.columns(12);
		create().comboBox(Employee.REPORTSTO_FK)
						.preferredWidth(120)
						// Only transfer focus backward on Enter, this way
						// the Enter key without any modifiers will trigger
						// the default dialog button, for inserting and updating
						.transferFocusOnEnter(BACKWARD);

		JPanel firstLastNamePanel = gridLayoutPanel(1, 2)
						.add(create().inputPanel(Employee.FIRSTNAME))
						.add(create().inputPanel(Employee.LASTNAME))
						.build();

		JPanel birthHireDatePanel = gridLayoutPanel(1, 2)
						.add(create().inputPanel(Employee.BIRTHDATE))
						.add(create().inputPanel(Employee.HIREDATE))
						.build();

		JPanel cityPostalCodePanel = flexibleGridLayoutPanel(1, 2)
						.add(create().inputPanel(Employee.CITY))
						.add(create().inputPanel(Employee.POSTALCODE))
						.build();

		JPanel stateCountryPanel = flexibleGridLayoutPanel(1, 2)
						.add(create().inputPanel(Employee.STATE))
						.add(create().inputPanel(Employee.COUNTRY))
						.build();

		setLayout(flexibleGridLayout(4, 3));
		add(firstLastNamePanel);
		add(birthHireDatePanel);
		addInputPanel(Employee.TITLE);
		addInputPanel(Employee.ADDRESS);
		add(cityPostalCodePanel);
		add(stateCountryPanel);
		addInputPanel(Employee.PHONE);
		addInputPanel(Employee.FAX);
		addInputPanel(Employee.EMAIL);
		addInputPanel(Employee.REPORTSTO_FK);
	}
}