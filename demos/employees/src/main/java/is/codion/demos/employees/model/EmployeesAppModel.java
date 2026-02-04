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
package is.codion.demos.employees.model;

import is.codion.demos.employees.domain.Employees.Department;
import is.codion.demos.employees.domain.Employees.Employee;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

import java.util.List;

// tag::applicationModel[]
public final class EmployeesAppModel extends SwingEntityApplicationModel {

	public EmployeesAppModel(EntityConnectionProvider connectionProvider) {
		super(connectionProvider, List.of(createDepartmentModel(connectionProvider)));
	}

	private static SwingEntityModel createDepartmentModel(EntityConnectionProvider connectionProvider) {
		SwingEntityModel departmentModel = new SwingEntityModel(Department.TYPE, connectionProvider);
		EmployeeEditModel employeeEditModel = new EmployeeEditModel(connectionProvider);
		//Refresh the manager ComboBoxModel when an employee is added, deleted or updated,
		//in case a new manager got hired, fired or promoted
		employeeEditModel.afterInsertUpdateOrDelete().addListener(employeeEditModel.editor()
						.comboBoxModels().get(Employee.MANAGER_FK).items()::refresh);
		departmentModel.detailModels().add(new SwingEntityModel(employeeEditModel));
		departmentModel.tableModel().items().refresh();

		return departmentModel;
	}
}
// end::applicationModel[]