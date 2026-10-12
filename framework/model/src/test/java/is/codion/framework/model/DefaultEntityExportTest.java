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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.state.State;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityResultIterator;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityExport.Attributes;
import is.codion.framework.model.ExportDomain.Department;
import is.codion.framework.model.ExportDomain.Employee;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.framework.domain.entity.OrderBy.ascending;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DefaultEntityExportTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new ExportDomain())
					.user(UNIT_TEST_USER)
					.build();
	private static final Entities ENTITIES = CONNECTION.entities();

	@Test
	void basicExport() {
		EntityConnection connection = CONNECTION;
		// no included attributes, an export of nothing is a configuration error
		StringBuilder empty = new StringBuilder();
		assertThrows(IllegalStateException.class, () -> EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(attributes -> {})
						.entities(connection.select(Select.all(Employee.TYPE)
														.orderBy(ascending(Employee.NAME))
														.build())
										.iterator())
						.output(empty::append)
						.export());
		assertEquals("", empty.toString());

		Set<Entity.Key> jones = singleton(connection.selectSingle(Employee.NAME.equalTo("JONES")).primaryKey());

		StringBuilder output = new StringBuilder();
		EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(employee -> employee.add(Employee.NAME))
						.keys(jones.iterator())
						.output(output::append)
						.export();
		assertEquals("Name\nJONES\n", output.toString());

		output = new StringBuilder();
		EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(employee -> employee
										.add(Employee.NAME)
										.add(Employee.JOB)
										.add(Employee.COMMISSION)
										.add(Employee.HIREDATE))
						.keys(jones.iterator())
						.output(output::append)
						.export();
		assertEquals("Name\tJob\tCommission\tHiredate\nJONES\tMANAGER\t\t04-02-1981\n", output.toString());

		output = new StringBuilder();
		EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(employee -> employee
										.add(Employee.DEPARTMENT)
										.add(Employee.NAME)
										.add(Employee.JOB)
										.add(Employee.MGR))
						.keys(jones.iterator())
						.output(output::append)
						.export();
		assertEquals("deptno\tName\tJob\tmgr\n20\tJONES\tMANAGER\t8\n", output.toString());
	}

	@Test
	void invalidPaths() {
		assertThrows(IllegalArgumentException.class, () -> EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(attributes -> attributes.add()));
		// an attribute of another entity
		assertThrows(IllegalArgumentException.class, () -> EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(attributes -> attributes.add(Department.NAME)));
		// only the last attribute may be other than a foreign key
		assertThrows(IllegalArgumentException.class, () -> EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(attributes -> attributes.add(Employee.NAME, Employee.JOB)));
		// an attribute not of the referenced entity
		assertThrows(IllegalArgumentException.class, () -> EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(attributes -> attributes.add(Employee.DEPARTMENT_FK, Employee.NAME)));
	}

	@Test
	void anyOrder() {
		Set<Entity.Key> jones = singleton(CONNECTION.selectSingle(Employee.NAME.equalTo("JONES")).primaryKey());

		// the attributes of a referenced entity need not follow each other
		StringBuilder output = new StringBuilder();
		EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(employee -> employee
										.add(Employee.DEPARTMENT_FK, Department.NAME)
										.add(Employee.NAME)
										.add(Employee.MGR_FK, Employee.NAME)
										.add(Employee.DEPARTMENT_FK, Department.LOCATION)
										.add(Employee.DEPARTMENT_FK))
						.keys(jones.iterator())
						.output(output::append)
						.export();
		assertEquals("Department Name\tName\tManager Name\tDepartment Location\tDepartment\n" +
						"RESEARCH\tJONES\tKING\tDALLAS\tRESEARCH\n", output.toString());
	}

	@Test
	void derived() {
		StringBuilder output = new StringBuilder();
		EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(attributes -> attributes
										.add(Employee.DEPARTMENT_LOCATION)
										.add(Employee.INITIAL)
										.add(Employee.INITIAL_LOWER))
						.keys(singletonList(ENTITIES.key(Employee.TYPE)
										.with(Employee.ID, 3)// Jones
										.build()).iterator())
						.output(output::append)
						.export();
		assertEquals("Location\tinitial\tinitial_lower\nDALLAS\tJ\tj\n", output.toString());
	}

	@Test
	void employee() throws IOException {
		StringBuilder output = new StringBuilder();
		EntityConnection connection = CONNECTION;
		try (EntityResultIterator iterator = connection.iterator(Select.all(Employee.TYPE)
						.orderBy(ascending(Employee.NAME))
						.build())) {
			EntityExport.builder(CONNECTION)
							.entityType(Employee.TYPE)
							.attributes(employee -> captionOrder(Employee.TYPE).forEach(employee::add))
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
		EntityConnection connection = CONNECTION;
		try (EntityResultIterator iterator = connection.iterator(Select.all(Employee.TYPE)
						.orderBy(ascending(Employee.NAME))
						.build())) {
			EntityExport.builder(CONNECTION)
							.entityType(Employee.TYPE)
							.attributes(employee -> employee
											.add(Employee.DEPARTMENT_FK, Department.LOCATION)
											.add(Employee.DEPARTMENT_FK, Department.NAME)
											.add(Employee.NAME))
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
		EntityConnection connection = CONNECTION;
		try (EntityResultIterator iterator = connection.iterator(Select.all(Employee.TYPE)
						.orderBy(ascending(Employee.NAME))
						.build())) {
			EntityExport.builder(CONNECTION)
							.entityType(Employee.TYPE)
							.attributes(employee -> employee
											.add(Employee.MGR_FK, Employee.DEPARTMENT_FK, Department.LOCATION)
											.add(Employee.MGR_FK, Employee.DEPARTMENT_FK, Department.NAME)
											.add(Employee.MGR_FK, Employee.NAME)
											.add(Employee.NAME))
							.entities(iterator)
							.output(output::append)
							.export();
		}

		String exportResult = textFileContents(DefaultEntityExportTest.class, "employee_manager_department.tsv");
		assertEquals(exportResult, output.toString());
	}

	@Test
	void test() throws IOException {
		StringBuilder output = new StringBuilder();
		AtomicInteger count = new AtomicInteger();
		EntityConnection connection = CONNECTION;
		try (EntityResultIterator iterator = connection.iterator(Select.all(Employee.TYPE)
						.orderBy(ascending(Employee.NAME))
						.build())) {
			EntityExport.builder(CONNECTION)
							.entityType(Employee.TYPE)
							.attributes(employee -> captionOrder(employee, emptyList(), Employee.TYPE, asList(
											singletonList(Employee.DEPARTMENT_FK),
											singletonList(Employee.MGR_FK),
											asList(Employee.MGR_FK, Employee.MGR_FK))))
							.entities(iterator)
							.output(output::append)
							.processed(entity -> count.getAndIncrement())
							.cancel(State.state())
							.export();
		}

		String exportResult = textFileContents(DefaultEntityExportTest.class, "employee_export.tsv");
		assertEquals(exportResult, output.toString());

		Entity jones = CONNECTION.selectSingle(Employee.NAME.equalTo("JONES"));
		Entity accounting = CONNECTION.selectSingle(Department.ID.equalTo(10));

		List<Entity.Key> keys = asList(jones.primaryKey(), accounting.primaryKey());

		assertThrows(IllegalArgumentException.class, () -> EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(employee -> captionOrder(Employee.TYPE).forEach(employee::add))
						.keys(keys.iterator())
						.output(line -> {})
						.export());
	}

	@Test
	void loadedForeignKeyIsNotSelected() {
		//a department which does not exist in the database, selecting it would throw
		Entity department = ENTITIES.entity(Department.TYPE)
						.with(Department.ID, -42)
						.with(Department.NAME, "Ministry")
						.with(Department.LOCATION, "Elsewhere")
						.build();
		Entity employee = ENTITIES.entity(Employee.TYPE)
						.with(Employee.ID, -42)
						.with(Employee.NAME, "AGENT")
						.with(Employee.DEPARTMENT_FK, department)
						.build();

		StringBuilder output = new StringBuilder();
		EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(attributes -> attributes
										.add(Employee.DEPARTMENT_FK, Department.LOCATION)
										.add(Employee.DEPARTMENT_FK, Department.NAME)
										.add(Employee.NAME))
						.entities(singletonList(employee).iterator())
						.output(output::append)
						.export();

		assertEquals("Department Location\tDepartment Name\tName\nElsewhere\tMinistry\tAGENT\n", output.toString());
	}

	@Test
	void partiallyLoadedForeignKeyIsSelected() {
		//the department is present, but was not selected with the LOCATION the export requires
		Entity department = ENTITIES.entity(Department.TYPE)
						.with(Department.ID, 10)
						.with(Department.NAME, "Stale name")
						.build();
		Entity employee = ENTITIES.entity(Employee.TYPE)
						.with(Employee.ID, -42)
						.with(Employee.NAME, "AGENT")
						.with(Employee.DEPARTMENT_FK, department)
						.build();

		StringBuilder output = new StringBuilder();
		EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(attributes -> attributes
										.add(Employee.DEPARTMENT_FK, Department.LOCATION)
										.add(Employee.DEPARTMENT_FK, Department.NAME)
										.add(Employee.NAME))
						.entities(singletonList(employee).iterator())
						.output(output::append)
						.export();

		assertEquals("Department Location\tDepartment Name\tName\nNEW YORK\tACCOUNTING\tAGENT\n", output.toString());
	}

	/**
	 * @return the attributes of the given entity type which are not hidden, by caption
	 */
	private static List<Attribute<?>> captionOrder(EntityType entityType) {
		return ENTITIES.definition(entityType).attributes().definitions().stream()
						.filter(definition -> !definition.hidden())
						.sorted(comparing(AttributeDefinition::caption))
						.map(AttributeDefinition::attribute)
						.collect(toList());
	}

	/**
	 * Adds the attributes of the given entity type which are not hidden, by caption, the given foreign keys followed
	 * by the attributes of the entity they reference, the order the export had before the attributes were ordered as
	 * added, which the expected output files follow.
	 */
	private static void captionOrder(Attributes attributes, List<Attribute<?>> parent, EntityType entityType,
																	 Collection<List<Attribute<?>>> foreignKeys) {
		for (Attribute<?> attribute : captionOrder(entityType)) {
			List<Attribute<?>> path = new ArrayList<>(parent);
			path.add(attribute);
			attributes.add(path);
			if (foreignKeys.contains(path)) {
				captionOrder(attributes, path, ((ForeignKey) attribute).referencedType(), foreignKeys);
			}
		}
	}

	private static String textFileContents(Class<?> resourceClass, String resourceName) throws IOException {
		try (BufferedReader input = new BufferedReader(new InputStreamReader(resourceClass.getResourceAsStream(resourceName)))) {
			return input.lines().collect(joining("\n")) + "\n";
		}
	}
}
