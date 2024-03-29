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

import is.codion.common.model.loadtest.LoadTest.Scenario.Performer;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.framework.model.SwingEntityModel;

import java.util.HashMap;
import java.util.Map;

import static is.codion.framework.domain.entity.test.EntityTestUtil.createRandomEntity;
import static is.codion.swing.framework.model.tools.loadtest.EntityLoadTestUtil.selectRandomRow;

// tag::loadTest[]
public final class InsertEmployee implements Performer<EmployeesAppModel> {

	@Override
	public void perform(EmployeesAppModel application) throws Exception {
		SwingEntityModel departmentModel = application.entityModel(Department.TYPE);
		selectRandomRow(departmentModel.tableModel());
		SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
		Map<ForeignKey, Entity> foreignKeyEntities = new HashMap<>();
		foreignKeyEntities.put(Employee.DEPARTMENT_FK, departmentModel.tableModel().selectionModel().getSelectedItem());
		employeeModel.editModel().set(createRandomEntity(application.entities(), Employee.TYPE, foreignKeyEntities));
		employeeModel.editModel().insert();
	}
}
// end::loadTest[]