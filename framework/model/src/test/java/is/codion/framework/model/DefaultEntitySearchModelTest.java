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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.model.DefaultEntitySearchModel.DefaultBuilder;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntitySearchModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Entities ENTITIES = new TestDomain().entities();

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	private EntitySearchModel searchModel;
	private Collection<Column<String>> searchable;

	@Test
	void constructorNullEntityType() {
		assertThrows(NullPointerException.class, () -> new DefaultBuilder(null, CONNECTION_PROVIDER));
	}

	@Test
	void constructorNullConnectionProvider() {
		assertThrows(NullPointerException.class, () -> new DefaultBuilder(Employee.TYPE, null));
	}

	@Test
	void constructorNullColumns() {
		assertThrows(NullPointerException.class, () -> new DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER).searchColumns(null));
	}

	@Test
	void searchWithNoColumns() {
		assertThrows(IllegalArgumentException.class, () -> new DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER).searchColumns(emptyList()));
	}

	@Test
	void constructorIncorrectEntityColumn() {
		assertThrows(IllegalArgumentException.class, () -> new DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER)
						.searchColumns(singleton(Department.NAME)));
	}

	@Test
	void theRest() {
		assertNotNull(searchModel.connectionProvider());
		assertTrue(searchModel.columns().containsAll(searchable));
	}

	@Test
	void wrongEntityType() {
		assertThrows(IllegalArgumentException.class, () -> searchModel.selection().entity().set(ENTITIES.entity(Department.TYPE)));
	}

	@Test
	void singleSelection() {
		EntitySearchModel model = new DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER)
						.singleSelection(true)
						.build();
		List<Entity> entities = asList(ENTITIES.entity(Employee.TYPE), ENTITIES.entity(Employee.TYPE));
		assertThrows(IllegalArgumentException.class, () -> model.selection().entities().set(entities));
	}

	@Test
	void searchModel() {
		searchModel.search().strings().set(singleton("joh"));
		assertTrue(searchModel.selection().empty().get());
		List<Entity> result = searchModel.search().result();
		assertFalse(result.isEmpty());
		assertTrue(contains(result, "John"));
		assertTrue(contains(result, "johnson"));
		assertFalse(contains(result, "Andy"));
		assertFalse(contains(result, "Andrew"));
		assertEquals(singleton("joh"), searchModel.search().strings().get());
		searchModel.selection().entities().set(result);
		assertFalse(searchModel.selection().empty().get());

		searchModel.search().strings().set(asList("joh", "and"));
		result = searchModel.search().result();
		assertFalse(result.isEmpty());
		assertTrue(contains(result, "John"));
		assertTrue(contains(result, "johnson"));
		assertTrue(contains(result, "Andy"));
		assertTrue(contains(result, "Andrew"));

		searchModel.search().strings().set(singleton("jo"));
		result = searchModel.search().result();
		assertTrue(contains(result, "John"));
		assertTrue(contains(result, "johnson"));
		assertFalse(contains(result, "Andy"));
		assertFalse(contains(result, "Andrew"));

		searchModel.search().strings().set(singleton("le"));
		result = searchModel.search().result();
		assertTrue(contains(result, "John"));
		assertFalse(contains(result, "johnson"));
		assertTrue(contains(result, "Andy"));
		assertFalse(contains(result, "Andrew"));

		searchModel.settings().get(Employee.NAME).wildcardPrefix().set(false);
		searchModel.settings().get(Employee.JOB).wildcardPrefix().set(false);
		searchModel.search().strings().set(asList("jo", "cl"));
		result = searchModel.search().result();
		assertTrue(contains(result, "John"));
		assertTrue(contains(result, "johnson"));
		assertTrue(contains(result, "Andy"));
		assertFalse(contains(result, "Andrew"));

		searchModel.search().strings().set(singleton("Joh"));
		searchModel.settings().get(Employee.NAME).caseSensitive().set(true);
		searchModel.settings().get(Employee.JOB).caseSensitive().set(true);
		result = searchModel.search().result();
		assertEquals(1, result.size());
		assertTrue(contains(result, "John"));
		assertFalse(contains(result, "johnson"));
		searchModel.settings().get(Employee.NAME).wildcardPrefix().set(false);
		searchModel.settings().get(Employee.JOB).wildcardPrefix().set(false);
		searchModel.settings().get(Employee.NAME).caseSensitive().set(false);
		searchModel.settings().get(Employee.JOB).caseSensitive().set(false);
		result = searchModel.search().result();
		assertTrue(contains(result, "John"));
		assertTrue(contains(result, "johnson"));
		assertFalse(contains(result, "Andy"));
		assertFalse(contains(result, "Andrew"));

		searchModel.search().strings().set(asList("andy ", " Andrew "));//spaces should be trimmed away
		result = searchModel.search().result();
		assertEquals(2, result.size());
		assertTrue(contains(result, "Andy"));
		assertTrue(contains(result, "Andrew"));

		searchModel.search().strings().set(asList("andy", "Andrew"));
		result = searchModel.search().result();
		assertEquals(2, result.size());
		assertTrue(contains(result, "Andy"));
		assertTrue(contains(result, "Andrew"));
		searchModel.selection().entities().set(result);

		searchModel.search().strings().set(asList("and", " rew"));
		searchModel.settings().get(Employee.NAME).wildcardPrefix().set(true);
		searchModel.settings().get(Employee.JOB).wildcardPrefix().set(true);
		searchModel.settings().get(Employee.NAME).wildcardPostfix().set(false);
		searchModel.settings().get(Employee.JOB).wildcardPostfix().set(false);
		result = searchModel.search().result();
		assertEquals(1, result.size());
		assertFalse(contains(result, "Andy"));
		assertTrue(contains(result, "Andrew"));

		searchModel.search().strings().set(singleton("Joh"));
		searchModel.settings().get(Employee.NAME).caseSensitive().set(true);
		searchModel.settings().get(Employee.JOB).caseSensitive().set(true);
		searchModel.settings().get(Employee.NAME).wildcardPostfix().set(true);
		searchModel.settings().get(Employee.JOB).wildcardPostfix().set(true);
		searchModel.condition().set(() -> Employee.JOB.notEqualTo("MANAGER"));
		result = searchModel.search().result();
		assertTrue(contains(result, "John"));
		assertFalse(contains(result, "johnson"));

		searchModel.condition().clear();
		searchModel.settings().get(Employee.NAME).caseSensitive().set(false);
		searchModel.search().strings().set(singleton("jo on"));// space as wildcard
		result = searchModel.search().result();
		assertEquals(1, result.size());
		assertTrue(contains(result, "johnson"));
	}

	@Test
	void condition() {
		searchModel.search().strings().set(singleton("johnson"));
		List<Entity> result = searchModel.search().result();
		assertEquals(1, result.size());
		searchModel.selection().entities().set(result);
		searchModel.condition().set(Employee.CONDITION_1_TYPE::get);
		assertEquals(1, searchModel.selection().entities().get().size());
		result = searchModel.search().result();
		assertTrue(result.isEmpty());
		searchModel.condition().set(() -> null);
		assertThrows(IllegalArgumentException.class, searchModel.search()::result);
		searchModel.condition().set(Department.NAME::isNotNull);
		assertThrows(IllegalArgumentException.class, searchModel.search()::result);
		searchModel.condition().clear();
	}

	@Test
	void limit() {
		searchModel.search().strings().set(singleton("j"));
		assertEquals(4, searchModel.search().result().size());
		searchModel.limit().set(3);
		assertEquals(3, searchModel.search().result().size());
		searchModel.limit().clear();
		assertEquals(4, searchModel.search().result().size());
	}

	@Test
	void attributes() {
		DefaultBuilder builder = new DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER);
		assertThrows(IllegalArgumentException.class, () -> builder.attributes(singleton(Department.NAME)));

		EntitySearchModel model = builder
						.searchColumns(searchable)
						.attributes(singleton(Employee.NAME))
						.build();
		model.search().strings().set(singleton("John"));
		Entity john = model.search().result().get(0);
		assertTrue(john.contains(Employee.ID));
		assertTrue(john.contains(Employee.NAME));
		ENTITIES.definition(Employee.TYPE).attributes().get().stream()
						.filter(attribute -> !(attribute.equals(Employee.NAME) || attribute.equals(Employee.ID)))
						.forEach(attribute -> assertFalse(john.contains(attribute)));
	}

	@Test
	void orderBy() {
		DefaultBuilder builder = new DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER);
		assertThrows(IllegalArgumentException.class, () -> builder.orderBy(OrderBy.ascending(Department.NAME)));

		EntitySearchModel model = builder
						.orderBy(OrderBy.descending(Employee.NAME))
						.build();
		model.search().strings().set(singleton("Jo"));
		List<Entity> result = model.search().result();
		assertEquals(3, result.size());
		assertEquals("John", result.get(0).get(Employee.NAME));
		assertEquals("johnson", result.get(1).get(Employee.NAME));
		assertEquals("JONES", result.get(2).get(Employee.NAME));
	}

	@Test
	void editEvents() {
		Entity temp = ENTITIES.builder(Employee.TYPE)
						.with(Employee.ID, -42)
						.with(Employee.NAME, "Noname")
						.build();

		searchModel.selection().entity().set(temp);

		temp.put(Employee.NAME, "Newname");
		Entity tempUpdated = temp.copy().mutable();
		tempUpdated.save(Employee.NAME);

		Map<Entity, Entity> updated = new HashMap<>();
		updated.put(temp, tempUpdated);

		EntityEditModel.editEvents().updated(Employee.TYPE).accept(updated);
		assertEquals("Newname", searchModel.selection().entity().get().get(Employee.NAME));

		EntityEditModel.editEvents().deleted(Employee.TYPE).accept(singletonList(temp));
		assertTrue(searchModel.selection().empty().get());
	}

	@BeforeEach
	void setUp() {
		searchable = asList(Employee.NAME, Employee.JOB);
		searchModel = new DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER)
						.searchColumns(searchable)
						.build();

		CONNECTION_PROVIDER.connection().startTransaction();
		setupData();
	}

	@AfterEach
	void tearDown() {
		CONNECTION_PROVIDER.connection().rollbackTransaction();
	}

	private static boolean contains(List<Entity> result, String employeeName) {
		return result.stream().anyMatch(entity -> entity.get(Employee.NAME).equals(employeeName));
	}

	private static void setupData() {
		Entity dept = ENTITIES.builder(Department.TYPE)
						.with(Department.ID, 88)
						.with(Department.LOCATION, "TestLoc")
						.with(Department.NAME, "TestDept")
						.build();

		Entity emp = ENTITIES.builder(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, dept)
						.with(Employee.COMMISSION, 1000d)
						.with(Employee.HIREDATE, LocalDate.now())
						.with(Employee.JOB, "CLERK")
						.with(Employee.NAME, "John")
						.with(Employee.SALARY, 1000d)
						.build();

		Entity emp2 = ENTITIES.builder(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, dept)
						.with(Employee.COMMISSION, 1000d)
						.with(Employee.HIREDATE, LocalDate.now())
						.with(Employee.JOB, "MANAGER")
						.with(Employee.NAME, "johnson")
						.with(Employee.SALARY, 1000d)
						.build();

		Entity emp3 = ENTITIES.builder(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, dept)
						.with(Employee.COMMISSION, 1000d)
						.with(Employee.HIREDATE, LocalDate.now())
						.with(Employee.JOB, "CLERK")
						.with(Employee.NAME, "Andy")
						.with(Employee.SALARY, 1000d)
						.build();

		Entity emp4 = ENTITIES.builder(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, dept)
						.with(Employee.COMMISSION, 1000d)
						.with(Employee.HIREDATE, LocalDate.now())
						.with(Employee.JOB, "MANAGER")
						.with(Employee.NAME, "Andrew")
						.with(Employee.SALARY, 1000d)
						.build();

		CONNECTION_PROVIDER.connection().insert(asList(dept, emp, emp2, emp3, emp4));
	}
}
