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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.DefaultEntitySearchModel.DefaultBuilder;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntitySearchModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Entities ENTITIES = new TestDomain().entities();

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	private EntitySearchModel searchModel;
	private Collection<Column<String>> searchable;

	@Test
	void builderValidation() {
		// Null checks
		assertThrows(NullPointerException.class, () -> new DefaultBuilder(null, CONNECTION));
		assertThrows(NullPointerException.class, () -> new DefaultBuilder(Employee.TYPE, null));
		assertThrows(NullPointerException.class, () -> new DefaultBuilder(Employee.TYPE, CONNECTION).search((Column<String>[]) null));
		// Invalid arguments
		assertThrows(IllegalArgumentException.class, () -> new DefaultBuilder(Employee.TYPE, CONNECTION).search(emptyList()));
		assertThrows(IllegalArgumentException.class, () -> new DefaultBuilder(Employee.TYPE, CONNECTION)
						.search(singleton(Department.NAME)));
	}

	@Test
	void theRest() {
		assertNotNull(searchModel.connection());
		assertTrue(searchModel.columns().containsAll(searchable));
	}

	@Test
	void wrongEntityType() {
		assertThrows(IllegalArgumentException.class, () -> searchModel.selection().entity().set(ENTITIES.entity(Department.TYPE).build()));
	}

	@Test
	void selection() {
		assertFalse(searchModel.selection().present().is());

		searchModel.search().string().set("joh");
		List<Entity> result = searchModel.search().perform();

		List<Entity> notified = new ArrayList<>();
		searchModel.selection().entity().addConsumer(notified::add);

		searchModel.selection().entity().set(result.get(0));
		assertTrue(searchModel.selection().present().is());
		assertEquals(result.get(0), searchModel.selection().entity().get());
		// the same instance again does not notify
		searchModel.selection().entity().set(result.get(0));
		assertEquals(1, notified.size());

		searchModel.selection().clear();
		assertFalse(searchModel.selection().present().is());
		assertNull(searchModel.selection().entity().get());
		// nor does clearing an empty selection
		searchModel.selection().clear();
		assertEquals(2, notified.size());
	}

	@Test
	void searchModel() {
		searchModel.search().string().set("joh");
		assertFalse(searchModel.selection().present().is());
		List<Entity> result = searchModel.search().perform();
		assertFalse(result.isEmpty());
		assertTrue(contains(result, "John"));
		assertTrue(contains(result, "johnson"));
		assertFalse(contains(result, "Andy"));
		assertFalse(contains(result, "Andrew"));
		searchModel.selection().entity().set(result.get(0));
		assertTrue(searchModel.selection().present().is());

		searchModel.search().string().set("Joh");
		searchModel.settings().get(Employee.NAME).caseSensitive().set(true);
		searchModel.settings().get(Employee.JOB).caseSensitive().set(true);
		result = searchModel.search().perform();
		assertEquals(1, result.size());
		assertTrue(contains(result, "John"));
		assertFalse(contains(result, "johnson"));
		searchModel.settings().get(Employee.NAME).wildcardPrefix().set(false);
		searchModel.settings().get(Employee.JOB).wildcardPrefix().set(false);
		searchModel.settings().get(Employee.NAME).caseSensitive().set(false);
		searchModel.settings().get(Employee.JOB).caseSensitive().set(false);
		result = searchModel.search().perform();
		assertTrue(contains(result, "John"));
		assertTrue(contains(result, "johnson"));
		assertFalse(contains(result, "Andy"));
		assertFalse(contains(result, "Andrew"));

		searchModel.search().string().set(" Andrew ");//spaces should be trimmed away
		result = searchModel.search().perform();
		assertEquals(1, result.size());
		assertTrue(contains(result, "Andrew"));

		searchModel.search().string().set("andy");
		result = searchModel.search().perform();
		assertEquals(1, result.size());
		assertTrue(contains(result, "Andy"));
		searchModel.selection().entity().set(result.get(0));

		searchModel.search().string().set(" rew");
		searchModel.settings().get(Employee.NAME).wildcardPrefix().set(true);
		searchModel.settings().get(Employee.JOB).wildcardPrefix().set(true);
		searchModel.settings().get(Employee.NAME).wildcardPostfix().set(false);
		searchModel.settings().get(Employee.JOB).wildcardPostfix().set(false);
		result = searchModel.search().perform();
		assertEquals(1, result.size());
		assertTrue(contains(result, "Andrew"));

		searchModel.search().string().set("Joh");
		searchModel.settings().get(Employee.NAME).caseSensitive().set(true);
		searchModel.settings().get(Employee.JOB).caseSensitive().set(true);
		searchModel.settings().get(Employee.NAME).wildcardPostfix().set(true);
		searchModel.settings().get(Employee.JOB).wildcardPostfix().set(true);
		searchModel.condition().set(() -> Employee.JOB.notEqualTo("MANAGER"));
		result = searchModel.search().perform();
		assertTrue(contains(result, "John"));
		assertFalse(contains(result, "johnson"));

		searchModel.condition().clear();
		searchModel.settings().get(Employee.NAME).caseSensitive().set(false);
		searchModel.search().string().set("jo on");// space as wildcard
		result = searchModel.search().perform();
		assertEquals(1, result.size());
		assertTrue(contains(result, "johnson"));
	}

	@Test
	void foreignKeyFilter() {
		Entity testDept = CONNECTION.selectSingle(Department.ID.equalTo(88));
		Entity sales = CONNECTION.selectSingle(Department.NAME.equalTo("SALES"));
		Entity john = CONNECTION.selectSingle(Employee.NAME.equalTo("John"));
		int all = CONNECTION.select(Condition.all(Employee.TYPE)).size();
		int inSales = CONNECTION.select(Employee.DEPARTMENT_FK.equalTo(sales)).size();
		// everything matches, the filter decides
		searchModel.search().string().set("%");
		assertEquals(all, searchModel.search().perform().size());

		assertThrows(IllegalArgumentException.class, () -> searchModel.filter().get(Detail.MASTER_FK));
		ForeignKeyFilter filter = searchModel.filter().get(Employee.DEPARTMENT_FK);
		assertSame(filter, searchModel.filter().get(Employee.DEPARTMENT_FK));
		assertTrue(filter.get().isEmpty());
		assertTrue(filter.strict().is());
		assertThrows(IllegalArgumentException.class, () -> filter.set(john.primaryKey()));

		filter.set(testDept.primaryKey());
		List<Entity> result = searchModel.search().perform();
		assertEquals(4, result.size());
		assertTrue(result.stream().allMatch(employee -> employee.entity(Employee.DEPARTMENT_FK).equals(testDept)));

		filter.set(asList(testDept.primaryKey(), sales.primaryKey()));
		result = searchModel.search().perform();
		assertEquals(4 + inSales, result.size());
		assertEquals(2, filter.get().size());

		// strict, no keys, nothing, without a query
		filter.set(emptyList());
		assertTrue(searchModel.search().perform().isEmpty());
		// not strict, no keys, unfiltered
		filter.strict().set(false);
		assertEquals(all, searchModel.search().perform().size());
		filter.set(testDept.primaryKey());
		assertEquals(4, searchModel.search().perform().size());
		filter.strict().set(true);
		// not strict, entities with a null reference included
		Entity blake = CONNECTION.selectSingle(Employee.NAME.equalTo("BLAKE"));
		int underBlake = CONNECTION.select(Employee.MGR_FK.equalTo(blake)).size();
		int withoutManager = CONNECTION.select(Employee.MGR_FK.isNull()).size();
		filter.clear();
		ForeignKeyFilter managerFilter = searchModel.filter().get(Employee.MGR_FK);
		managerFilter.set(blake.primaryKey());
		assertEquals(underBlake, searchModel.search().perform().size());
		managerFilter.strict().set(false);
		assertEquals(underBlake + withoutManager, searchModel.search().perform().size());
		managerFilter.clear();
		filter.set(testDept.primaryKey());
		assertEquals(4, searchModel.search().perform().size());

		// AND'ed to the search condition and the additional condition
		searchModel.search().string().set("joh");
		assertEquals(2, searchModel.search().perform().size());
		searchModel.condition().set(() -> Employee.JOB.equalTo("MANAGER"));
		assertEquals(1, searchModel.search().perform().size());
		searchModel.condition().clear();

		filter.clear();
		assertTrue(filter.get().isEmpty());
		assertEquals(2, searchModel.search().perform().size());
		searchModel.search().string().set("%");
		assertEquals(all, searchModel.search().perform().size());
	}

	@Test
	void foreignKeyFilterLink() {
		Entity accounting = CONNECTION.selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		Entity testDept = CONNECTION.selectSingle(Department.ID.equalTo(88));
		int inAccounting = CONNECTION.select(Employee.DEPARTMENT_FK.equalTo(accounting)).size();
		EntityComboBoxModel departments = EntityComboBoxModel.builder()
						.entityType(Department.TYPE)
						.connection(CONNECTION)
						.build();
		departments.items().refresh();
		departments.selection().item().set(accounting);
		EntityComboBoxModel employees = EntityComboBoxModel.builder()
						.entityType(Employee.TYPE)
						.connection(CONNECTION)
						.build();
		assertThrows(IllegalArgumentException.class, () -> searchModel.filter().get(Employee.DEPARTMENT_FK).link(employees));

		searchModel.search().string().set("%");
		ForeignKeyFilter filter = searchModel.filter().get(Employee.DEPARTMENT_FK);
		filter.link(departments);
		// the master selection filters
		assertEquals(singleton(accounting.primaryKey()), new HashSet<>(filter.get()));
		List<Entity> result = searchModel.search().perform();
		assertEquals(inAccounting, result.size());
		assertTrue(result.stream().allMatch(employee -> employee.entity(Employee.DEPARTMENT_FK).equals(accounting)));
		departments.selection().item().set(testDept);
		assertEquals(4, searchModel.search().perform().size());
		// strict, no master selection, nothing
		departments.selection().item().clear();
		assertTrue(filter.get().isEmpty());
		assertTrue(searchModel.search().perform().isEmpty());
		// the entity selected selects the one it references in the master
		Entity john = CONNECTION.selectSingle(Employee.NAME.equalTo("John"));
		searchModel.selection().entity().set(john);
		assertEquals(testDept, departments.selection().item().get());
		assertEquals(singleton(testDept.primaryKey()), new HashSet<>(filter.get()));

		// linked when built
		EntitySearchModel built = new DefaultBuilder(Employee.TYPE, CONNECTION)
						.search(searchable)
						.filter(Employee.DEPARTMENT_FK, departments)
						.build();
		built.search().string().set("%");
		assertEquals(4, built.search().perform().size());
		assertThrows(IllegalArgumentException.class, () -> new DefaultBuilder(Employee.TYPE, CONNECTION)
						.filter(Employee.DEPARTMENT_FK, employees));

		// a search model as the master
		EntitySearchModel departmentSearch = new DefaultBuilder(Department.TYPE, CONNECTION)
						.search(Department.NAME)
						.build();
		EntitySearchModel bySearch = new DefaultBuilder(Employee.TYPE, CONNECTION)
						.search(searchable)
						.filter(Employee.DEPARTMENT_FK, departmentSearch)
						.build();
		bySearch.search().string().set("%");
		// strict, no master selection, nothing
		assertTrue(bySearch.search().perform().isEmpty());
		departmentSearch.selection().entity().set(accounting);
		assertEquals(inAccounting, bySearch.search().perform().size());
		bySearch.selection().entity().set(john);
		assertEquals(testDept, departmentSearch.selection().entity().get());
		assertEquals(4, bySearch.search().perform().size());
		assertThrows(IllegalArgumentException.class, () -> bySearch.filter().get(Employee.DEPARTMENT_FK).link(searchModel));
		assertThrows(IllegalArgumentException.class, () -> new DefaultBuilder(Employee.TYPE, CONNECTION)
						.filter(Employee.DEPARTMENT_FK, searchModel));
	}

	@Test
	void condition() {
		searchModel.search().string().set("johnson");
		List<Entity> result = searchModel.search().perform();
		assertEquals(1, result.size());
		searchModel.selection().entity().set(result.get(0));
		searchModel.condition().set(Employee.CONDITION_1_TYPE::get);
		assertTrue(searchModel.selection().present().is());
		result = searchModel.search().perform();
		assertTrue(result.isEmpty());
		searchModel.condition().set(() -> null);
		assertThrows(IllegalArgumentException.class, searchModel.search()::perform);
		searchModel.condition().set(Department.NAME::isNotNull);
		assertThrows(IllegalArgumentException.class, searchModel.search()::perform);
		searchModel.condition().clear();
	}

	@Test
	void limit() {
		searchModel.search().string().set("j");
		assertEquals(4, searchModel.search().perform().size());
		searchModel.limit().set(3);
		assertEquals(3, searchModel.search().perform().size());
		searchModel.limit().clear();
		assertEquals(4, searchModel.search().perform().size());
	}

	@Test
	void settingsDefaults() {
		EntitySearchModel.Settings settings = new DefaultBuilder(Employee.TYPE, CONNECTION)
						.search(searchable)
						.build()
						.settings().get(Employee.NAME);
		assertTrue(settings.wildcardPrefix().is());
		assertTrue(settings.wildcardPostfix().is());
		assertTrue(settings.spaceAsWildcard().is());
		assertFalse(settings.caseSensitive().is());

		EntitySearchModel.WILDCARD_PREFIX.set(false);
		EntitySearchModel.WILDCARD_POSTFIX.set(false);
		EntitySearchModel.SPACE_AS_WILDCARD.set(false);
		EntitySearchModel.CASE_SENSITIVE.set(true);
		try {
			settings = new DefaultBuilder(Employee.TYPE, CONNECTION)
							.search(searchable)
							.build()
							.settings().get(Employee.NAME);
			assertFalse(settings.wildcardPrefix().is());
			assertFalse(settings.wildcardPostfix().is());
			assertFalse(settings.spaceAsWildcard().is());
			assertTrue(settings.caseSensitive().is());
		}
		finally {
			EntitySearchModel.WILDCARD_PREFIX.clear();
			EntitySearchModel.WILDCARD_POSTFIX.clear();
			EntitySearchModel.SPACE_AS_WILDCARD.clear();
			EntitySearchModel.CASE_SENSITIVE.clear();
		}
	}

	@Test
	void attributes() {
		DefaultBuilder builder = new DefaultBuilder(Employee.TYPE, CONNECTION);
		assertThrows(IllegalArgumentException.class, () -> builder.attributes(singleton(Department.NAME)));

		EntitySearchModel model = builder
						.search(searchable)
						.attributes(singleton(Employee.NAME))
						.build();
		model.search().string().set("John");
		Entity john = model.search().perform().get(0);
		assertTrue(john.contains(Employee.ID));
		assertTrue(john.contains(Employee.NAME));
		ENTITIES.definition(Employee.TYPE).attributes().get().stream()
						.filter(attribute -> !(attribute.equals(Employee.NAME) || attribute.equals(Employee.ID) || attribute.equals(Employee.DATA)))
						.forEach(attribute -> assertFalse(john.contains(attribute)));
	}

	@Test
	void orderBy() {
		DefaultBuilder builder = new DefaultBuilder(Employee.TYPE, CONNECTION);
		assertThrows(IllegalArgumentException.class, () -> builder.orderBy(OrderBy.ascending(Department.NAME)));

		EntitySearchModel model = builder
						.orderBy(OrderBy.descending(Employee.NAME))
						.build();
		model.search().string().set("Jo");
		List<Entity> result = model.search().perform();
		assertEquals(3, result.size());
		assertEquals("John", result.get(0).get(Employee.NAME));
		assertEquals("johnson", result.get(1).get(Employee.NAME));
		assertEquals("JONES", result.get(2).get(Employee.NAME));
	}

	@Test
	void persistenceAware() {
		Entity temp = ENTITIES.entity(Employee.TYPE)
						.with(Employee.ID, -42)
						.with(Employee.NAME, "Noname")
						.build();

		searchModel.selection().entity().set(temp);

		temp.set(Employee.NAME, "Newname");
		Entity tempUpdated = temp.copy().mutable();
		tempUpdated.save(Employee.NAME);

		Map<Entity, Entity> updated = new HashMap<>();
		updated.put(temp, tempUpdated);

		PersistenceEvents persistenceEvents = PersistenceEvents.persistenceEvents(Employee.TYPE);
		persistenceEvents.updated().accept(updated);
		assertEquals("Newname", searchModel.selection().entity().get().get(Employee.NAME));

		persistenceEvents.deleted().accept(singletonList(temp));
		assertFalse(searchModel.selection().present().is());
	}

	@Test
	void persistenceAwareUpdateReplacesTheSelectedEntity() {
		Entity selected = ENTITIES.entity(Employee.TYPE)
						.with(Employee.ID, -42)
						.with(Employee.NAME, "Noname")
						.build();
		searchModel.selection().entity().set(selected);
		List<Entity> notified = new ArrayList<>();
		searchModel.selection().entity().addConsumer(notified::add);

		// the primary key modified as well, matched on its original value
		Entity beforeUpdate = selected.copy().mutable();
		beforeUpdate.set(Employee.ID, -44);
		beforeUpdate.set(Employee.NAME, "Newname");
		Entity afterUpdate = beforeUpdate.copy().mutable();
		afterUpdate.save();
		Map<Entity, Entity> updated = new HashMap<>();
		updated.put(beforeUpdate, afterUpdate);
		PersistenceEvents.persistenceEvents(Employee.TYPE).updated().accept(updated);

		assertSame(afterUpdate, searchModel.selection().entity().get());
		assertEquals(singletonList(afterUpdate), notified);

		// an update or delete of another entity leaves the selection alone
		Entity other = ENTITIES.entity(Employee.TYPE)
						.with(Employee.ID, -43)
						.with(Employee.NAME, "Another")
						.build();
		Entity otherUpdated = other.copy().mutable();
		otherUpdated.set(Employee.NAME, "Changed");
		Map<Entity, Entity> otherUpdates = new HashMap<>();
		otherUpdates.put(otherUpdated, otherUpdated.copy().mutable());
		PersistenceEvents.persistenceEvents(Employee.TYPE).updated().accept(otherUpdates);
		PersistenceEvents.persistenceEvents(Employee.TYPE).deleted().accept(singletonList(other));
		assertEquals(1, notified.size());
		assertSame(afterUpdate, searchModel.selection().entity().get());
	}

	@BeforeEach
	void setUp() {
		searchable = asList(Employee.NAME, Employee.JOB);
		searchModel = new DefaultBuilder(Employee.TYPE, CONNECTION)
						.search(searchable)
						.build();

		CONNECTION.startTransaction();
		setupData();
	}

	@AfterEach
	void tearDown() {
		CONNECTION.rollbackTransaction();
	}

	private static boolean contains(List<Entity> result, String employeeName) {
		return result.stream().anyMatch(entity -> entity.get(Employee.NAME).equals(employeeName));
	}

	private static void setupData() {
		Entity dept = ENTITIES.entity(Department.TYPE)
						.with(Department.ID, 88)
						.with(Department.LOCATION, "TestLoc")
						.with(Department.NAME, "TestDept")
						.build();

		Entity emp = ENTITIES.entity(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, dept)
						.with(Employee.COMMISSION, 1000d)
						.with(Employee.HIREDATE, LocalDate.now())
						.with(Employee.JOB, "CLERK")
						.with(Employee.NAME, "John")
						.with(Employee.SALARY, 1000d)
						.build();

		Entity emp2 = ENTITIES.entity(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, dept)
						.with(Employee.COMMISSION, 1000d)
						.with(Employee.HIREDATE, LocalDate.now())
						.with(Employee.JOB, "MANAGER")
						.with(Employee.NAME, "johnson")
						.with(Employee.SALARY, 1000d)
						.build();

		Entity emp3 = ENTITIES.entity(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, dept)
						.with(Employee.COMMISSION, 1000d)
						.with(Employee.HIREDATE, LocalDate.now())
						.with(Employee.JOB, "CLERK")
						.with(Employee.NAME, "Andy")
						.with(Employee.SALARY, 1000d)
						.build();

		Entity emp4 = ENTITIES.entity(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, dept)
						.with(Employee.COMMISSION, 1000d)
						.with(Employee.HIREDATE, LocalDate.now())
						.with(Employee.JOB, "MANAGER")
						.with(Employee.NAME, "Andrew")
						.with(Employee.SALARY, 1000d)
						.build();

		CONNECTION.insert(asList(dept, emp, emp2, emp3, emp4));
	}
}
