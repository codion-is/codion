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

import is.codion.framework.db.EntityConnection;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.test.DefaultEntityFactory;
import is.codion.framework.domain.test.DomainTest.EntityFactory;
import is.codion.swing.framework.model.SwingEntityModel;

import java.util.Random;

// tag::loadTest[]
public final class UpdateEmployee extends AbstractPerformer {

	private final Random random = new Random();

	@Override
	public void perform(EmployeesAppModel application) {
		SwingEntityModel departmentModel = application.entityModel(Department.TYPE);
		selectRandomRow(departmentModel.tableModel());
		SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
		EntityFactory entityFactory = new DefaultEntityFactory(application.connection());
		if (employeeModel.tableModel().items().visible().count() > 0) {
			EntityConnection connection = employeeModel.connection();
			connection.startTransaction();
			try {
				selectRandomRow(employeeModel.tableModel());
				Entity selected = employeeModel.tableModel().selection().item().get();
				entityFactory.modify(selected);
				employeeModel.editModel().entity().set(selected);
				employeeModel.editModel().update();
				selectRandomRow(employeeModel.tableModel());
				selected = employeeModel.tableModel().selection().item().get();
				entityFactory.modify(selected);
				employeeModel.editModel().entity().set(selected);
				employeeModel.editModel().update();
			}
			finally {
				if (random.nextBoolean()) {
					connection.rollbackTransaction();
				}
				else {
					connection.commitTransaction();
				}
			}
		}
	}
}
// end::loadTest[]