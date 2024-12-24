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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model.test;

import is.codion.common.Operator;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A base class for testing {@link EntityTableModel} subclasses.
 * @param <EditModel> the {@link EntityEditModel} type
 * @param <TableModel> the {@link EntityTableModel} type
 */
public abstract class AbstractEntityTableModelTest<EditModel extends EntityEditModel, TableModel extends EntityTableModel<EditModel>> {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	private final EntityConnectionProvider connectionProvider;

	protected final List<Entity> testEntities = initTestEntities(CONNECTION_PROVIDER.entities());

	protected final TableModel testModel;

	protected AbstractEntityTableModelTest() {
		connectionProvider = CONNECTION_PROVIDER;
		testModel = createTestTableModel();
	}

	@Test
	public void select() {
		TableModel tableModel = createTableModel(Employee.TYPE, connectionProvider);
		tableModel.refresh();

		List<Entity.Key> keys = tableModel.entities().primaryKeys(Employee.TYPE, 1, 2);
		Entity.Key pk1 = keys.get(0);
		Entity.Key pk2 = keys.get(1);

		tableModel.select(singletonList(pk1));
		Entity selectedPK1 = tableModel.selection().item().get();
		assertEquals(pk1, selectedPK1.primaryKey());
		assertEquals(1, tableModel.selection().count());

		tableModel.select(singletonList(pk2));
		Entity selectedPK2 = tableModel.selection().item().get();
		assertEquals(pk2, selectedPK2.primaryKey());
		assertEquals(1, tableModel.selection().count());

		tableModel.select(keys);
		List<Entity> selectedItems = tableModel.selection().items().get();
		for (Entity selected : selectedItems) {
			assertTrue(keys.contains(selected.primaryKey()));
		}
		assertEquals(2, tableModel.selection().count());
	}

	@Test
	public void selectedEntitiesIterator() {
		TableModel tableModel = createTableModel(Employee.TYPE, connectionProvider);
		tableModel.refresh();

		tableModel.selection().indexes().set(asList(0, 3, 5));
		Iterator<Entity> iterator = tableModel.selection().items().get().iterator();
		assertEquals(tableModel.items().visible().get().get(0), iterator.next());
		assertEquals(tableModel.items().visible().get().get(3), iterator.next());
		assertEquals(tableModel.items().visible().get().get(5), iterator.next());
	}

	@Test
	public void onInsert() {
		TableModel deptModel = createDepartmentTableModel();
		deptModel.refresh();

		Entities entities = deptModel.entities();
		deptModel.onInsert().set(EntityTableModel.OnInsert.ADD_BOTTOM);
		Entity dept = entities.builder(Department.TYPE)
						.with(Department.ID, -10)
						.with(Department.LOCATION, "Nowhere1")
						.with(Department.NAME, "HELLO")
						.build();
		int count = deptModel.items().visible().count();
		deptModel.editModel().insert(singletonList(dept));
		assertEquals(count + 1, deptModel.items().visible().count());
		assertEquals(dept, deptModel.items().visible().get().get(deptModel.items().visible().count() - 1));

		deptModel.onInsert().set(EntityTableModel.OnInsert.ADD_TOP_SORTED);
		Entity dept2 = entities.builder(Department.TYPE)
						.with(Department.ID, -20)
						.with(Department.LOCATION, "Nowhere2")
						.with(Department.NAME, "NONAME")
						.build();
		deptModel.editModel().insert(singletonList(dept2));
		assertEquals(count + 2, deptModel.items().visible().count());
		assertEquals(dept2, deptModel.items().visible().get().get(2));

		deptModel.onInsert().set(EntityTableModel.OnInsert.DO_NOTHING);
		Entity dept3 = entities.builder(Department.TYPE)
						.with(Department.ID, -30)
						.with(Department.LOCATION, "Nowhere3")
						.with(Department.NAME, "NONAME2")
						.build();
		deptModel.editModel().insert(singletonList(dept3));
		assertEquals(count + 2, deptModel.items().visible().count());

		deptModel.refresh();
		assertEquals(count + 3, deptModel.items().visible().count());

		deptModel.editModel().delete(asList(dept, dept2, dept3));
	}

	@Test
	public void removeDeletedEntities() {
		TableModel tableModel = createTableModel(Employee.TYPE, connectionProvider);
		tableModel.refresh();

		Entities entities = tableModel.entities();
		Entity.Key pk1 = entities.primaryKey(Employee.TYPE, 1);
		Entity.Key pk2 = entities.primaryKey(Employee.TYPE, 2);
		tableModel.connection().startTransaction();
		try {
			tableModel.select(singletonList(pk1));
			tableModel.selection().index().set(0);
			Entity selected = tableModel.selection().item().get();
			tableModel.removeDeleted().set(true);
			tableModel.deleteSelected();
			assertFalse(tableModel.items().contains(selected));

			tableModel.select(singletonList(pk2));
			selected = tableModel.selection().item().get();
			tableModel.removeDeleted().set(false);
			assertEquals(1, tableModel.deleteSelected().size());
			assertTrue(tableModel.items().contains(selected));
		}
		finally {
			tableModel.connection().rollbackTransaction();
		}
	}

	@Test
	public void entityType() {
		assertEquals(Detail.TYPE, testModel.entityType());
	}

	@Test
	public void deleteNotEnabled() {
		testModel.editModel().deleteEnabled().set(false);
		testModel.refresh();
		testModel.selection().indexes().set(singletonList(0));
		assertThrows(IllegalStateException.class, testModel::deleteSelected);
	}

	@Test
	public void testTheRest() {
		assertNotNull(testModel.connectionProvider());
		assertNotNull(testModel.editModel());
		assertFalse(testModel.editModel().readOnly().get());
		testModel.refresh();
	}

	@Test
	public void attributes() {
		TableModel tableModel = createTableModel(Employee.TYPE, connectionProvider);
		assertTrue(tableModel.queryModel().attributes().get().isEmpty());
		tableModel.queryModel().attributes().addAll(Employee.NAME, Employee.HIREDATE);
		tableModel.refresh();
		assertTrue(tableModel.items().visible().count() > 0);
		tableModel.items().get().forEach(employee -> {
			assertFalse(employee.contains(Employee.COMMISSION));
			assertFalse(employee.contains(Employee.DEPARTMENT));
			assertTrue(employee.contains(Employee.NAME));
			assertTrue(employee.contains(Employee.HIREDATE));
		});
		assertThrows(IllegalArgumentException.class, () -> tableModel.queryModel().attributes().add(Department.NAME));
	}

	@Test
	public void limit() {
		TableModel tableModel = createTableModel(Employee.TYPE, connectionProvider);
		tableModel.queryModel().limit().set(6);
		tableModel.refresh();
		assertEquals(6, tableModel.items().visible().count());
		ConditionModel<Double> commissionCondition =
						tableModel.queryModel().conditions().attribute(Employee.COMMISSION);
		commissionCondition.operator().set(Operator.EQUAL);
		commissionCondition.enabled().set(true);
		tableModel.refresh();
		commissionCondition.enabled().set(false);
		tableModel.refresh();
		assertEquals(6, tableModel.items().visible().count());
		tableModel.queryModel().limit().clear();
		tableModel.refresh();
		assertEquals(16, tableModel.items().visible().count());
	}

	@Test
	public void conditionChangedListener() {
		TableModel empModel = createTableModel(Employee.TYPE, connectionProvider);
		AtomicInteger counter = new AtomicInteger();
		Runnable conditionChangedListener = counter::incrementAndGet;
		empModel.queryModel().conditionChanged().addListener(conditionChangedListener);
		ConditionModel<Double> commissionModel =
						empModel.queryModel().conditions().attribute(Employee.COMMISSION);
		commissionModel.enabled().set(true);
		assertEquals(1, counter.get());
		commissionModel.enabled().set(false);
		assertEquals(2, counter.get());
		commissionModel.operator().set(Operator.GREATER_THAN_OR_EQUAL);
		commissionModel.operands().lower().set(1200d);
		//automatically set enabled when upper bound is set
		assertEquals(3, counter.get());
		empModel.queryModel().conditionChanged().removeListener(conditionChangedListener);
	}

	@Test
	public void testSearchState() {
		TableModel empModel = createTableModel(Employee.TYPE, connectionProvider);
		assertFalse(empModel.queryModel().conditionChanged().get());
		ConditionModel<String> jobModel =
						empModel.queryModel().conditions().attribute(Employee.JOB);
		jobModel.operands().equal().set("job");
		assertTrue(empModel.queryModel().conditionChanged().get());
		jobModel.enabled().set(false);
		assertFalse(empModel.queryModel().conditionChanged().get());
		jobModel.enabled().set(true);
		assertTrue(empModel.queryModel().conditionChanged().get());
		empModel.refresh();
		assertFalse(empModel.queryModel().conditionChanged().get());
	}

	protected final EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	/**
	 * @return a EntityTableModel using {@link #testEntities} with an edit model
	 * @see Detail#TYPE
	 */
	protected abstract TableModel createTestTableModel();

	protected abstract TableModel createDepartmentTableModel();

	protected abstract TableModel createTableModel(EntityType entityType, EntityConnectionProvider connectionProvider);

	protected abstract TableModel createTableModel(EditModel editModel);

	protected abstract EditModel createEditModel(EntityType entityType, EntityConnectionProvider connectionProvider);

	private static List<Entity> initTestEntities(Entities entities) {
		List<Entity> testEntities = new ArrayList<>(5);
		String[] stringValues = new String[] {"a", "b", "c", "d", "e"};
		for (int i = 0; i < 5; i++) {
			testEntities.add(entities.builder(Detail.TYPE)
							.with(Detail.ID, (long) i + 1)
							.with(Detail.INT, i + 1)
							.with(Detail.STRING, stringValues[i])
							.build());
		}

		return testEntities;
	}
}