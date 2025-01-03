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
package is.codion.demos.employees.testing;

import is.codion.common.user.User;
import is.codion.demos.employees.domain.Employees;
import is.codion.demos.employees.domain.Employees.Department;
import is.codion.demos.employees.domain.Employees.Employee;
import is.codion.demos.employees.model.EmployeesAppModel;
import is.codion.demos.employees.testing.scenarios.InsertDepartment;
import is.codion.demos.employees.testing.scenarios.InsertEmployee;
import is.codion.demos.employees.testing.scenarios.LoginLogout;
import is.codion.demos.employees.testing.scenarios.SelectDepartment;
import is.codion.demos.employees.testing.scenarios.UpdateEmployee;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.tools.loadtest.LoadTest;

import java.util.List;
import java.util.function.Function;

import static is.codion.tools.loadtest.LoadTest.Scenario.scenario;
import static is.codion.tools.loadtest.model.LoadTestModel.loadTestModel;
import static is.codion.tools.loadtest.ui.LoadTestPanel.loadTestPanel;

// tag::loadTest[]
public final class EmployeesLoadTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final class EmployeesAppModelFactory
					implements Function<User, EmployeesAppModel> {

		@Override
		public EmployeesAppModel apply(User user) {
			EmployeesAppModel applicationModel =
							new EmployeesAppModel(EntityConnectionProvider.builder()
											.domainType(Employees.DOMAIN)
											.clientType(EmployeesLoadTest.class.getSimpleName())
											.user(user)
											.build());

			SwingEntityModel model = applicationModel.entityModels().get(Department.TYPE);
			model.detailModels().link(model.detailModels().get(Employee.TYPE)).active().set(true);
			model.tableModel().items().refresh();

			return applicationModel;
		}
	}

	public static void main(String[] args) {
		LoadTest<EmployeesAppModel> loadTest =
						LoadTest.builder(new EmployeesAppModelFactory(),
														application -> application.connectionProvider().close())
										.user(UNIT_TEST_USER)
										.scenarios(List.of(
														scenario(new InsertDepartment(), 1),
														scenario(new InsertEmployee(), 3),
														scenario(new LoginLogout(), 4),
														scenario(new SelectDepartment(), 10),
														scenario(new UpdateEmployee(), 5)))
										.name("Employees LoadTest - " + EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get())
										.build();
		loadTestPanel(loadTestModel(loadTest)).run();
	}
}
// end::loadTest[]