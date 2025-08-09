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
package is.codion.demos.employees.ui;

import is.codion.demos.employees.domain.Employees.Department;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

// tag::constructor[]
public class DepartmentEditPanel extends EntityEditPanel {

	public DepartmentEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
	}
	// end::constructor[]

	// tag::initializeUI[]
	@Override
	protected void initializeUI() {
		focus().initial().set(Department.DEPARTMENT_NO);

		createTextField(Department.DEPARTMENT_NO)
						.columns(3)
						//don't allow editing of existing department numbers
						.enabled(editModel().editor().exists().not());
		createTextField(Department.NAME)
						.columns(8);
		createTextField(Department.LOCATION)
						.columns(12);

		editModel().editor().exists().addConsumer(exists ->
						focus().initial().set(exists ? Department.NAME : Department.DEPARTMENT_NO));

		setLayout(borderLayout());
		add(borderLayoutPanel()
						.northComponent(borderLayoutPanel()
										.westComponent(createInputPanel(Department.DEPARTMENT_NO))
										.centerComponent(createInputPanel(Department.NAME)))
						.centerComponent(createInputPanel(Department.LOCATION))
						.build(), BorderLayout.CENTER);
	}
}
// end::initializeUI[]