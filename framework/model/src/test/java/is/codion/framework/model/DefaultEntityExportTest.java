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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.state.State;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.EntityResultIterator;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.model.ExportDomain.Department;
import is.codion.framework.model.ExportDomain.Employee;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.framework.domain.entity.OrderBy.ascending;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DefaultEntityExportTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new ExportDomain())
					.user(UNIT_TEST_USER)
					.build();
	private static final Entities ENTITIES = CONNECTION_PROVIDER.entities();

	@Test
	void basicExport() {
		StringBuilder output = new StringBuilder();
		EntityConnection connection = CONNECTION_PROVIDER.connection();
		EntityExport.builder(CONNECTION_PROVIDER)
						.entityType(Employee.TYPE)
						.attributes(builder -> {})
						.entities(connection.select(Select.all(Employee.TYPE)
														.orderBy(ascending(Employee.NAME))
														.build())
										.iterator())
						.output(output::append)
						.export();
		// no included attributes
		assertEquals("", output.toString());

		assertThrows(IllegalArgumentException.class, () -> EntityExport.builder(CONNECTION_PROVIDER)
						.entityType(Employee.TYPE)
						.attributes(attributes -> attributes.include(Department.NAME)));
		assertThrows(IllegalArgumentException.class, () -> EntityExport.builder(CONNECTION_PROVIDER)
						.entityType(Employee.TYPE)
						.attributes(attributes -> attributes.order(Department.NAME)));

		Set<Entity.Key> jones = singleton(connection.selectSingle(Employee.NAME.equalTo("JONES")).primaryKey());

		output = new StringBuilder();
		EntityExport.builder(CONNECTION_PROVIDER)
						.entityType(Employee.TYPE)
						.attributes(employee -> employee.include(Employee.NAME))
						.keys(jones.iterator())
						.output(output::append)
						.export();
		assertEquals("Name\nJONES\n", output.toString());

		output = new StringBuilder();
		EntityExport.builder(CONNECTION_PROVIDER)
						.entityType(Employee.TYPE)
						.attributes(employee -> employee.include(Employee.NAME, Employee.JOB))
						.keys(jones.iterator())
						.output(output::append)
						.export();
		assertEquals("Job	Name\nMANAGER	JONES\n", output.toString());

		output = new StringBuilder();
		EntityExport.builder(CONNECTION_PROVIDER)
						.entityType(Employee.TYPE)
						.attributes(employee -> employee
										.include(Employee.NAME, Employee.JOB, Employee.HIREDATE, Employee.COMMISSION)
										.order(Employee.NAME, Employee.JOB))
						.keys(jones.iterator())
						.output(output::append)
						.export();
		assertEquals("Name	Job	Commission	Hiredate\nJONES	MANAGER		04-02-1981\n", output.toString());

		output = new StringBuilder();
		EntityExport.builder(CONNECTION_PROVIDER)
						.entityType(Employee.TYPE)
						.attributes(employee -> employee
										.include(Employee.NAME, Employee.JOB, Employee.DEPARTMENT, Employee.MGR)
										.order(Employee.DEPARTMENT, Employee.NAME, Employee.JOB))
						.keys(jones.iterator())
						.output(output::append)
						.export();
		assertEquals("deptno	Name	Job	mgr\n20	JONES	MANAGER	8\n", output.toString());
	}

	@Test
	void employee() throws IOException {
		Collection<Attribute<?>> employeeAttributes = ENTITIES.definition(Employee.TYPE).attributes().definitions().stream()
						.filter(attributeDefinition -> !attributeDefinition.hidden())
						.map(AttributeDefinition::attribute)
						.collect(toList());

		StringBuilder output = new StringBuilder();
		EntityConnection connection = CONNECTION_PROVIDER.connection();
		try (EntityResultIterator iterator = connection.iterator(Select.all(Employee.TYPE)
						.orderBy(ascending(Employee.NAME))
						.build())) {
			EntityExport.builder(CONNECTION_PROVIDER)
							.entityType(Employee.TYPE)
							.attributes(employee -> employee.include(employeeAttributes))
							.entities(iterator)
							.output(output::append)
							.export();
		}

		String exportResult = textFileContents(DefaultEntityExportTest.class, "employee.tsv");
		assertEquals(exportResult, output.toString());
	}

	@Test
	void employeeDepartment() throws IOException {
		StringBuilder output = new StringBuilder();
		EntityConnection connection = CONNECTION_PROVIDER.connection();
		try (EntityResultIterator iterator = connection.iterator(Select.all(Employee.TYPE)
						.orderBy(ascending(Employee.NAME))
						.build())) {
			EntityExport.builder(CONNECTION_PROVIDER)
							.entityType(Employee.TYPE)
							.attributes(employee -> employee.include(Employee.NAME)
											.attributes(Employee.DEPARTMENT_FK, department ->
															department.include(Department.NAME, Department.LOCATION)))
							.entities(iterator)
							.output(output::append)
							.export();
		}

		String exportResult = textFileContents(DefaultEntityExportTest.class, "employee_department.tsv");
		assertEquals(exportResult, output.toString());
	}

	@Test
	void employeeManagerDepartmentLevel() throws IOException {
		StringBuilder output = new StringBuilder();
		EntityConnection connection = CONNECTION_PROVIDER.connection();
		try (EntityResultIterator iterator = connection.iterator(Select.all(Employee.TYPE)
						.orderBy(ascending(Employee.NAME))
						.build())) {
			EntityExport.builder(CONNECTION_PROVIDER)
							.entityType(Employee.TYPE)
							.attributes(employee -> employee
											.include(Employee.NAME)
											.attributes(Employee.MGR_FK, manager ->
															manager.include(Employee.NAME)
																			.attributes(Employee.DEPARTMENT_FK, managerDepartment ->
																							managerDepartment.include(Department.NAME, Department.LOCATION))))
							.entities(iterator)
							.output(output::append)
							.export();
		}

		String exportResult = textFileContents(DefaultEntityExportTest.class, "employee_manager_department.tsv");
		assertEquals(exportResult, output.toString());
	}

	@Test
	void test() throws IOException {
		Collection<Attribute<?>> employeeAttributes = ENTITIES.definition(Employee.TYPE).attributes().definitions().stream()
						.filter(attributeDefinition -> !attributeDefinition.hidden())
						.map(AttributeDefinition::attribute)
						.collect(toList());
		Collection<Attribute<?>> departmentAttributes = ENTITIES.definition(Department.TYPE).attributes().definitions().stream()
						.filter(attributeDefinition -> !attributeDefinition.hidden())
						.map(AttributeDefinition::attribute)
						.collect(toList());

		StringBuilder output = new StringBuilder();
		AtomicInteger count = new AtomicInteger();
		EntityConnection connection = CONNECTION_PROVIDER.connection();
		try (EntityResultIterator iterator = connection.iterator(Select.all(Employee.TYPE)
						.orderBy(ascending(Employee.NAME))
						.build())) {
			EntityExport.builder(CONNECTION_PROVIDER)
							.entityType(Employee.TYPE)
							.attributes(employee -> employee
											.include(employeeAttributes)
											.attributes(Employee.DEPARTMENT_FK, department ->
															department.include(departmentAttributes))
											.attributes(Employee.MGR_FK, manager ->
															manager.include(employeeAttributes)
																			.attributes(Employee.MGR_FK, managersManager ->
																							managersManager.include(employeeAttributes))))
							.entities(iterator)
							.output(output::append)
							.processed(entity -> count.getAndIncrement())
							.cancel(State.state())
							.export();
		}

		String exportResult = textFileContents(DefaultEntityExportTest.class, "employee_export.tsv");
		assertEquals(exportResult, output.toString());

		Entity jones = CONNECTION_PROVIDER.connection().selectSingle(Employee.NAME.equalTo("JONES"));
		Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.ID.equalTo(10));

		List<Entity.Key> keys = asList(jones.primaryKey(), accounting.primaryKey());

		assertThrows(IllegalArgumentException.class, () -> EntityExport.builder(CONNECTION_PROVIDER)
						.entityType(Employee.TYPE)
						.attributes(employee -> employee.include(employeeAttributes))
						.keys(keys.iterator())
						.output(line -> {})
						.export());
	}

	private static String textFileContents(Class<?> resourceClass, String resourceName) throws IOException {
		try (BufferedReader input = new BufferedReader(new InputStreamReader(resourceClass.getResourceAsStream(resourceName)))) {
			return input.lines().collect(joining("\n")) + "\n";
		}
	}
}
