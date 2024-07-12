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
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.test.DefaultEntityFactory;
import is.codion.swing.framework.model.SwingEntityModel;

// tag::loadTest[]
public final class InsertEmployee extends AbstractPerformer {

	@Override
	public void perform(EmployeesAppModel application) throws Exception {
		SwingEntityModel departmentModel = application.entityModel(Department.TYPE);
		selectRandomRow(departmentModel.tableModel());
		SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
		Entity employee = new DefaultEntityFactory(application.connection()).entity(Employee.TYPE);
		employee.put(Employee.DEPARTMENT_FK, departmentModel.tableModel().selectionModel().getSelectedItem());
		employeeModel.editModel().set(employee);
		employeeModel.editModel().insert();
	}
}
// end::loadTest[]