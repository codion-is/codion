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

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.demos.employees.domain.Employees;
import is.codion.demos.employees.domain.Employees.Department;
import is.codion.demos.employees.domain.Employees.Employee;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.http.HttpEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.tools.loadtest.LoadTest;
import is.codion.tools.loadtest.LoadTest.Scenario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static is.codion.framework.domain.entity.condition.Condition.all;
import static is.codion.tools.loadtest.LoadTest.Scenario.scenario;
import static is.codion.tools.loadtest.model.LoadTestModel.loadTestModel;
import static is.codion.tools.loadtest.ui.LoadTestPanel.loadTestPanel;

public final class EmployeesServletLoadTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Random RANDOM = new Random();

	private final LoadTest<EntityConnectionProvider> loadTest;

	private EmployeesServletLoadTest(User user) {
		loadTest = LoadTest.builder()
						.createApplication(EmployeesServletLoadTest::createApplication)
						.closeApplication(EmployeesServletLoadTest::closeApplication)
						.user(user)
						.scenarios(List.of(
										scenario(new SelectDepartment(), 4),
										scenario(new UpdateLocation(), 2),
										scenario(new SelectEmployees(), 5),
										scenario(new AddDepartment(), 1),
										scenario(new AddEmployee(), 4)))
						.minimumThinkTime(2500)
						.maximumThinkTime(5000)
						.loginDelayFactor(2)
						.applicationBatchSize(10)
						.build();
	}

	private static void closeApplication(EntityConnectionProvider client) {
		client.close();
	}

	private static EntityConnectionProvider createApplication(User user) throws CancelException {
		return HttpEntityConnectionProvider.builder()
						.clientType("EmployeesServletLoadTest")
						.domain(Employees.DOMAIN)
						.user(user)
						.build();
	}

	public static void main(String[] args) {
		loadTestPanel(loadTestModel(new EmployeesServletLoadTest(UNIT_TEST_USER).loadTest)).run();
	}

	private static final class UpdateLocation implements Scenario.Performer<EntityConnectionProvider> {

		@Override
		public void perform(EntityConnectionProvider client) {
			List<Entity> departments = client.connection().select(all(Department.TYPE));
			Entity entity = departments.get(new Random().nextInt(departments.size()));
			entity.set(Department.LOCATION, randomString(12));
			client.connection().update(entity);
		}
	}

	private static final class SelectDepartment implements Scenario.Performer<EntityConnectionProvider> {

		@Override
		public void perform(EntityConnectionProvider client) {
			client.connection().select(Department.NAME.equalTo("Accounting"));
		}
	}

	private static final class SelectEmployees implements Scenario.Performer<EntityConnectionProvider> {

		@Override
		public void perform(EntityConnectionProvider client) {
			List<Entity> departments = client.connection().select(all(Department.TYPE));

			client.connection().select(Employee.DEPARTMENT
							.equalTo(departments.get(new Random().nextInt(departments.size())).get(Department.DEPARTMENT_NO)));
		}
	}

	private static final class AddDepartment implements Scenario.Performer<EntityConnectionProvider> {

		@Override
		public void perform(EntityConnectionProvider client) {
			int departmentNo = new Random().nextInt(5000);
			client.connection().insert(client.entities().entity(Department.TYPE)
							.with(Department.DEPARTMENT_NO, departmentNo)
							.with(Department.NAME, randomString(6))
							.with(Department.LOCATION, randomString(8))
							.build());
		}
	}

	private static final class AddEmployee implements Scenario.Performer<EntityConnectionProvider> {

		private final Random random = new Random();

		@Override
		public void perform(EntityConnectionProvider client) {
			List<Entity> departments = client.connection().select(all(Department.TYPE));
			Entity department = departments.get(random.nextInt(departments.size()));
			client.connection().insert(client.entities().entity(Employee.TYPE)
							.with(Employee.DEPARTMENT_FK, department)
							.with(Employee.NAME, randomString(8))
							.with(Employee.JOB, Employee.JOB_ITEMS.get(random.nextInt(Employee.JOB_ITEMS.size())).get())
							.with(Employee.SALARY, BigDecimal.valueOf(random.nextInt(1000) + 1000))
							.with(Employee.HIREDATE, LocalDate.now())
							.with(Employee.COMMISSION, random.nextDouble() * 500)
							.build());
		}
	}

	private static String randomString(int length) {
		return RANDOM.ints(97, 122 + 1)
						.limit(length)
						.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
						.toString();
	}
}
