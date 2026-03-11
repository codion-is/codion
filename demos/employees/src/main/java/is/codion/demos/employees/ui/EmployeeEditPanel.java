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
package is.codion.demos.employees.ui;

import is.codion.demos.employees.domain.Employees.Department;
import is.codion.demos.employees.domain.Employees.Employee;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.component.EntityComboBox;

import javax.swing.JPanel;

import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;

// tag::constructor[]
public class EmployeeEditPanel extends EntityEditPanel {

	public EmployeeEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
	}
	// end::constructor[]

	// tag::initializeUI[]
	@Override
	protected void initializeUI() {
		create().textField(Employee.NAME)
						.columns(8);
		create().comboBox(Employee.DEPARTMENT_FK);
		create().itemComboBox(Employee.JOB);
		create().comboBox(Employee.MANAGER_FK);
		create().textField(Employee.SALARY)
						.columns(5);
		create().textField(Employee.COMMISSION)
						.columns(5);
		create().temporalFieldPanel(Employee.HIREDATE)
						.columns(7);

		setLayout(flexibleGridLayout(0, 3));

		addInputPanel(Employee.NAME);
		add(create().inputPanel(Employee.DEPARTMENT_FK)
						.component(createDepartmentPanel()));
		addInputPanel(Employee.JOB);

		addInputPanel(Employee.MANAGER_FK);
		add(gridLayoutPanel(1, 2)
						.add(create().inputPanel(Employee.SALARY))
						.add(create().inputPanel(Employee.COMMISSION)));
		addInputPanel(Employee.HIREDATE);
	}

	private JPanel createDepartmentPanel() {
		EntityComboBox departmentBox = (EntityComboBox) component(Employee.DEPARTMENT_FK).get();
		NumberField<Integer> departmentNumberField = departmentBox.selector().integerField(Department.DEPARTMENT_NO)
						.transferFocusOnEnter(true)
						.valid(editModel().editor().value(Employee.DEPARTMENT_FK).valid())
						.build();

		component(Employee.DEPARTMENT_FK).replace(departmentNumberField);

		return Components.borderLayoutPanel()
						.west(departmentNumberField)
						.center(departmentBox)
						.build();
	}
}
// end::initializeUI[]