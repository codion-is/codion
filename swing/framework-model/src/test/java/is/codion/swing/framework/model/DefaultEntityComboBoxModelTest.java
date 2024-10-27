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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.EntityEditEvents;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityComboBoxModelTest {

	private static final Entities ENTITIES = new TestDomain().entities();

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void editEvents() {
		EntityComboBoxModel comboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER).build();
		comboBoxModel.refresh();

		Entity temp = ENTITIES.builder(Employee.TYPE)
						.with(Employee.ID, -42)
						.with(Employee.NAME, "Noname")
						.build();

		EntityEditEvents.inserted(singletonList(temp));
		assertTrue(comboBoxModel.items().visible().contains(temp));

		temp.put(Employee.NAME, "Newname");
		temp.save(Employee.NAME);

		Map<Entity.Key, Entity> updated = new HashMap<>();
		updated.put(temp.primaryKey(), temp);

		EntityEditEvents.updated(updated);
		assertEquals("Newname", comboBoxModel.find(temp.primaryKey()).orElse(null).get(Employee.NAME));

		EntityEditEvents.deleted(singletonList(temp));
		assertFalse(comboBoxModel.items().visible().contains(temp));
	}

	@Test
	void constructorNullEntityType() {
		assertThrows(NullPointerException.class, () -> EntityComboBoxModel.builder(null, CONNECTION_PROVIDER).build());
	}

	@Test
	void constructorNullConnectionProvider() {
		assertThrows(NullPointerException.class, () -> EntityComboBoxModel.builder(Employee.TYPE, null));
	}

	@Test
	void foreignKeyFilter() {
		Entities entities = CONNECTION_PROVIDER.entities();
		EntityComboBoxModel employeeComboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER)
						.filterSelected(true)
						.build();
		EntityComboBoxModel managerComboBoxModel = employeeComboBoxModel.filter().get(Employee.MGR_FK).builder()
						.condition(() -> Employee.JOB.in("MANAGER", "PRESIDENT"))
						.filterSelected(true)
						.build();
		EntityComboBoxModel departmentComboBoxModel = managerComboBoxModel.filter().get(Employee.DEPARTMENT_FK).builder().build();
		employeeComboBoxModel.refresh();
		employeeComboBoxModel.filter().get(Employee.DEPARTMENT_FK).strict().set(true);
		assertEquals(0, employeeComboBoxModel.items().visible().count());
		assertEquals(16, employeeComboBoxModel.items().filtered().count());
		assertTrue(managerComboBoxModel.items().visible().contains(null));
		assertEquals(0, managerComboBoxModel.items().visible().count());
		assertEquals(4, managerComboBoxModel.items().filtered().count());
		assertFalse(departmentComboBoxModel.items().visible().contains(null));
		assertEquals(4, departmentComboBoxModel.items().visible().count());
		assertEquals(0, departmentComboBoxModel.items().filtered().count());

		departmentComboBoxModel.select(entities.primaryKey(Department.TYPE, 10));
		//three managers in the accounting department
		assertEquals(3, managerComboBoxModel.items().visible().count());
		assertEquals(1, managerComboBoxModel.items().filtered().count());

		managerComboBoxModel.select(entities.primaryKey(Employee.TYPE, 5));//Blake, Manager, Accounting
		assertEquals(5, employeeComboBoxModel.items().visible().count());
		assertEquals(11, employeeComboBoxModel.items().filtered().count());

		employeeComboBoxModel.select(entities.primaryKey(Employee.TYPE, 7));//Scott, Analyst, Research
		assertEquals(3, managerComboBoxModel.selection().item().get().get(Employee.ID));//Jones, Manager, Research
		assertEquals(20, departmentComboBoxModel.selection().item().get().get(Department.ID));// Research
		//one manager in the research department
		assertEquals(1, managerComboBoxModel.items().visible().count());
		assertEquals(3, managerComboBoxModel.items().filtered().count());
		//six employees under Jones
		assertEquals(6, employeeComboBoxModel.items().visible().count());
		assertEquals(10, employeeComboBoxModel.items().filtered().count());

		departmentComboBoxModel.select(entities.primaryKey(Department.TYPE, 40));// Operations
		//no managers or employees in the Operations department
		assertEquals(0, managerComboBoxModel.items().visible().count());
		assertEquals(4, managerComboBoxModel.items().filtered().count());
		assertEquals(0, employeeComboBoxModel.items().visible().count());
		assertEquals(16, employeeComboBoxModel.items().filtered().count());
	}

	@Test
	void foreignKeyFilterComboBoxModel() {
		EntityComboBoxModel employeeComboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER)
						.includeNull(true)
						.build();
		EntityComboBoxModel departmentComboBoxModel = employeeComboBoxModel.filter().get(Employee.DEPARTMENT_FK).builder().build();
		employeeComboBoxModel.refresh();//refreshes both
		assertFalse(departmentComboBoxModel.items().visible().contains(null));
		assertEquals(1, employeeComboBoxModel.getSize());
		Entity.Key accountingKey = CONNECTION_PROVIDER.entities().primaryKey(Department.TYPE, 10);
		departmentComboBoxModel.select(accountingKey);
		assertEquals(8, employeeComboBoxModel.getSize());
		departmentComboBoxModel.setSelectedItem(null);
		assertEquals(1, employeeComboBoxModel.getSize());
		Entity.Key salesKey = CONNECTION_PROVIDER.entities().primaryKey(Department.TYPE, 30);
		departmentComboBoxModel.select(salesKey);
		assertEquals(5, employeeComboBoxModel.getSize());
		employeeComboBoxModel.setSelectedItem(employeeComboBoxModel.items().visible().get().get(1));
		employeeComboBoxModel.setSelectedItem(null);
	}

	@Test
	void setForeignKeyFilterEntities() throws Exception {
		EntityComboBoxModel employeeComboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER).build();
		employeeComboBoxModel.refresh();
		Entity blake = employeeComboBoxModel.connectionProvider().connection().selectSingle(Employee.NAME.equalTo("BLAKE"));
		assertThrows(IllegalArgumentException.class, () -> employeeComboBoxModel.filter().get(Employee.DEPARTMENT_FK).set(blake.primaryKey()));

		employeeComboBoxModel.filter().get(Employee.MGR_FK).set(blake.primaryKey());
		assertEquals(5, employeeComboBoxModel.getSize());
		for (int i = 0; i < employeeComboBoxModel.getSize(); i++) {
			Entity item = employeeComboBoxModel.getElementAt(i);
			assertEquals(item.entity(Employee.MGR_FK), blake);
		}

		Entity sales = employeeComboBoxModel.connectionProvider().connection().selectSingle(Department.NAME.equalTo("SALES"));
		employeeComboBoxModel.filter().get(Employee.DEPARTMENT_FK).set(sales.primaryKey());
		assertEquals(2, employeeComboBoxModel.getSize());
		for (int i = 0; i < employeeComboBoxModel.getSize(); i++) {
			Entity item = employeeComboBoxModel.getElementAt(i);
			assertEquals(item.entity(Employee.DEPARTMENT_FK), sales);
			assertEquals(item.entity(Employee.MGR_FK), blake);
		}

		Entity accounting = employeeComboBoxModel.connectionProvider().connection().selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		EntityComboBoxModel deptComboBoxModel = employeeComboBoxModel.filter().get(Employee.DEPARTMENT_FK).builder().build();
		deptComboBoxModel.setSelectedItem(accounting);
		assertEquals(3, employeeComboBoxModel.getSize());
		for (int i = 0; i < employeeComboBoxModel.getSize(); i++) {
			Entity item = employeeComboBoxModel.getElementAt(i);
			assertEquals(item.entity(Employee.DEPARTMENT_FK), accounting);
			assertEquals(item.entity(Employee.MGR_FK), blake);
		}
		employeeComboBoxModel.items().get().stream()
						.filter(employee -> employee.entity(Employee.DEPARTMENT_FK).equals(accounting))
						.findFirst()
						.ifPresent(employeeComboBoxModel::setSelectedItem);
		assertEquals(accounting, deptComboBoxModel.selection().value());

		//non strict filtering
		employeeComboBoxModel.filter().get(Employee.MGR_FK).strict().set(false);//now shows employees without a manager, as in, Mr King
		employeeComboBoxModel.filter().get(Employee.DEPARTMENT_FK).clear();
		assertEquals(6, employeeComboBoxModel.getSize());
		boolean kingFound = false;
		for (int i = 0; i < employeeComboBoxModel.getSize(); i++) {
			Entity item = employeeComboBoxModel.getElementAt(i);
			if (Objects.equals(item.get(Employee.NAME), "KING")) {
				kingFound = true;
			}
			else {
				assertEquals(item.entity(Employee.MGR_FK), blake);
			}
		}
		assertTrue(kingFound);

		employeeComboBoxModel.filter().predicate().set(entity -> false);
		assertEquals(0, employeeComboBoxModel.getSize());
	}

	@Test
	void setSelectedEntityByKey() throws DatabaseException {
		EntityComboBoxModel comboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER).build();
		comboBoxModel.refresh();
		Entity clark = comboBoxModel.connectionProvider().connection().selectSingle(Employee.NAME.equalTo("CLARK"));
		comboBoxModel.select(clark.primaryKey());
		assertEquals(clark, comboBoxModel.selection().value());
		comboBoxModel.setSelectedItem(null);
		assertNull(comboBoxModel.selection().value());
		comboBoxModel.filter().predicate().set(entity -> false);
		comboBoxModel.select(clark.primaryKey());
		assertEquals(clark, comboBoxModel.selection().value());
		Entity.Key nobodyPK = ENTITIES.primaryKey(Employee.TYPE, -1);
		comboBoxModel.select(nobodyPK);
		assertEquals(clark, comboBoxModel.selection().value());
	}

	@Test
	void setSelectedEntityByPrimaryKeyNullValue() {
		EntityComboBoxModel comboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER).build();
		assertThrows(NullPointerException.class, () -> comboBoxModel.select(null));
	}

	@Test
	void selectorValue() {
		EntityComboBoxModel comboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER).build();
		comboBoxModel.refresh();
		assertThrows(IllegalArgumentException.class, () -> comboBoxModel.createSelectorValue(Department.ID));
		Value<Integer> empIdValue = comboBoxModel.createSelectorValue(Employee.ID);
		assertNull(empIdValue.get());
		Entity.Key jonesKey = comboBoxModel.connectionProvider().entities().primaryKey(Employee.TYPE, 5);
		comboBoxModel.select(jonesKey);
		assertEquals(5, empIdValue.get());
		comboBoxModel.setSelectedItem(null);
		assertNull(empIdValue.get());
		empIdValue.set(10);
		assertEquals("ADAMS", comboBoxModel.selection().value().get(Employee.NAME));
		empIdValue.clear();
		assertNull(comboBoxModel.selection().value());
	}

	@Test
	void attributes() {
		EntityComboBoxModel comboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER)
						.attributes(Arrays.asList(Employee.NAME, Employee.DEPARTMENT_FK))
						.build();
		comboBoxModel.refresh();
		for (Entity emp : comboBoxModel.items().get()) {
			assertTrue(emp.contains(Employee.ID));
			assertTrue(emp.contains(Employee.NAME));
			assertTrue(emp.contains(Employee.DEPARTMENT));
			assertTrue(emp.contains(Employee.DEPARTMENT_FK));
			assertFalse(emp.contains(Employee.JOB));
			assertFalse(emp.contains(Employee.MGR));
			assertFalse(emp.contains(Employee.COMMISSION));
			assertFalse(emp.contains(Employee.HIREDATE));
			assertFalse(emp.contains(Employee.SALARY));
		}
		comboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER).build();
		comboBoxModel.refresh();
		for (Entity emp : comboBoxModel.items().get()) {
			assertTrue(emp.contains(Employee.ID));
			assertTrue(emp.contains(Employee.NAME));
			assertTrue(emp.contains(Employee.DEPARTMENT));
			assertTrue(emp.contains(Employee.DEPARTMENT_FK));
			assertTrue(emp.contains(Employee.JOB));
			assertTrue(emp.contains(Employee.MGR));
			assertTrue(emp.contains(Employee.COMMISSION));
			assertTrue(emp.contains(Employee.HIREDATE));
			assertTrue(emp.contains(Employee.SALARY));
		}
	}

	@Test
	void test() throws DatabaseException {
		EntityComboBoxModel comboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER).build();
		AtomicInteger refreshed = new AtomicInteger();
		Runnable refreshListener = refreshed::incrementAndGet;
		comboBoxModel.refresher().success().addListener(refreshListener);
		assertEquals(Employee.TYPE, comboBoxModel.entityType());
		comboBoxModel.toString();
		assertEquals(0, comboBoxModel.getSize());
		assertNull(comboBoxModel.getSelectedItem());
		comboBoxModel.refresh();
		assertTrue(comboBoxModel.getSize() > 0);
		assertFalse(comboBoxModel.items().cleared());

		Entity clark = comboBoxModel.connectionProvider().connection().selectSingle(Employee.NAME.equalTo("CLARK"));
		comboBoxModel.setSelectedItem(clark);
		assertEquals(clark, comboBoxModel.selection().value());

		comboBoxModel.items().clear();
		assertEquals(0, comboBoxModel.getSize());

		comboBoxModel.condition().set(() -> Condition.custom(Employee.ENAME_CLARK));
		comboBoxModel.filter().get(Employee.DEPARTMENT_FK).set(singleton(ENTITIES.primaryKey(Department.TYPE, 10)));//accounting
		assertThrows(UnsupportedOperationException.class, () -> comboBoxModel.items().visible().predicate().set(entity -> false));

		comboBoxModel.refresh();
		assertEquals(1, comboBoxModel.getSize());
		assertEquals(2, refreshed.get());
		comboBoxModel.condition().clear();
		comboBoxModel.refresh();
		assertEquals(7, comboBoxModel.getSize());// 7 in acounting
		assertEquals(3, refreshed.get());
		comboBoxModel.refresher().success().removeListener(refreshListener);
	}

	@Test
	void orderBy() {
		EntityComboBoxModel comboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER)
						.orderBy(OrderBy.ascending(Employee.NAME))
						.build();
		comboBoxModel.refresh();
		assertEquals("ADAMS", comboBoxModel.getElementAt(0).get(Employee.NAME));
		comboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER)
						.orderBy(OrderBy.descending(Employee.NAME))
						.build();
		comboBoxModel.refresh();
		assertEquals("WARD", comboBoxModel.getElementAt(0).get(Employee.NAME));
	}

	@Test
	void comparator() throws DatabaseException {
		EntityComboBoxModel comboBoxModel = EntityComboBoxModel.builder(Department.TYPE, CONNECTION_PROVIDER)
						.comparator(Comparator.comparing(employee -> employee.get(Department.ID)))
						.build();
		comboBoxModel.refresh();

		EntityConnection connection = CONNECTION_PROVIDER.connection();

		assertEquals(0, comboBoxModel.items().visible().indexOf(connection.selectSingle(Department.NAME.equalTo("ACCOUNTING"))));
		assertEquals(1, comboBoxModel.items().visible().indexOf(connection.selectSingle(Department.NAME.equalTo("RESEARCH"))));
		assertEquals(2, comboBoxModel.items().visible().indexOf(connection.selectSingle(Department.NAME.equalTo("SALES"))));
		assertEquals(3, comboBoxModel.items().visible().indexOf(connection.selectSingle(Department.NAME.equalTo("OPERATIONS"))));
	}

	@Test
	void entity() {
		EntityComboBoxModel comboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER).build();
		comboBoxModel.refresh();
		Entity.Key allenPK = ENTITIES.primaryKey(Employee.TYPE, 1);
		assertNotNull(comboBoxModel.find(allenPK));
		Entity.Key nobodyPK = ENTITIES.primaryKey(Employee.TYPE, -1);
		assertFalse(comboBoxModel.find(nobodyPK).isPresent());
	}

	@Test
	void nullCaption() {
		EntityComboBoxModel model = EntityComboBoxModel.builder(Employee.TYPE, CONNECTION_PROVIDER)
						.includeNull(true)
						.build();
		model.refresh();
		assertTrue(model.items().contains(null));
		assertEquals("-", model.getSelectedItem().toString());
		assertNull(model.selection().value());
	}
}