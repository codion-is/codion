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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model.test;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.AbstractEntityEditModel;
import is.codion.framework.model.AbstractEntityModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.framework.model.ForeignKeyDetailModelLink;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A base class for testing {@link EntityModel} subclasses.
 * @param <Model> the {@link EntityModel} type
 * @param <EditModel> the {@link EntityEditModel} type
 * @param <TableModel> the {@link EntityTableModel} type
 */
public abstract class AbstractEntityModelTest<Model extends AbstractEntityModel<Model, EditModel, TableModel>,
				EditModel extends AbstractEntityEditModel, TableModel extends EntityTableModel<EditModel>> {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	protected static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	private final EntityConnectionProvider connectionProvider;

	protected final Model departmentModel;

	protected AbstractEntityModelTest() {
		connectionProvider = CONNECTION_PROVIDER;
		departmentModel = createDepartmentModel();
	}

	@Test
	public void testUpdatePrimaryKey() {
		if (!departmentModel.containsTableModel()) {
			return;
		}
		departmentModel.tableModel().items().refresh();
		EntityEditModel deptEditModel = departmentModel.editModel();
		TableModel deptTableModel = departmentModel.tableModel();
		Entity.Key operationsKey = deptEditModel.entities().primaryKey(Department.TYPE, 40);//operations
		deptTableModel.select(singletonList(operationsKey));

		assertTrue(deptTableModel.selection().empty().not().get());
		deptEditModel.value(Department.ID).set(80);
		assertFalse(deptTableModel.selection().empty().get());
		deptEditModel.update();

		assertFalse(deptTableModel.selection().empty().get());
		Entity operations = deptTableModel.selection().item().get();
		assertEquals(80, operations.get(Department.ID));

		deptTableModel.items().visible().predicate().set(item ->
						!Objects.equals(80, item.get(Department.ID)));

		deptEditModel.editor().set(operations);
		deptEditModel.value(Department.ID).set(40);
		deptEditModel.update();

		deptTableModel.items().filter();

		assertTrue(deptTableModel.items().filtered().get().isEmpty());
	}

	@Test
	public void testDetailModels() {
		assertEquals(1, departmentModel.detailModels().linked().size());
		departmentModel.detailModels().link(departmentModel.detailModels().get(Employee.TYPE)).active().set(false);
		assertTrue(departmentModel.detailModels().linked().isEmpty());
		departmentModel.detailModels().link(departmentModel.detailModels().get(Employee.TYPE)).active().set(true);
	}

	@Test
	public void detailModelNotFound() {
		assertThrows(IllegalArgumentException.class, () -> departmentModel.detailModels().get(Department.TYPE));
	}

	@Test
	public void clear() {
		if (!departmentModel.containsTableModel()) {
			return;
		}
		departmentModel.tableModel().items().refresh();
		assertTrue(departmentModel.tableModel().items().visible().count() > 0);

		Model employeeModel = departmentModel.detailModels().get(Employee.TYPE);
		employeeModel.tableModel().items().refresh();
		assertTrue(employeeModel.tableModel().items().visible().count() > 0);

		departmentModel.detailModels().get().forEach(detailModel -> detailModel.tableModel().items().clear());
		assertEquals(0, employeeModel.tableModel().items().visible().count());

		departmentModel.tableModel().items().clear();
		assertEquals(0, departmentModel.tableModel().items().visible().count());
	}

	@Test
	public void clearEditModelClearTableSelection() {
		if (!departmentModel.containsTableModel()) {
			return;
		}
		departmentModel.tableModel().items().refresh();
		departmentModel.tableModel().selection().indexes().set(asList(1, 2, 3));
		assertFalse(departmentModel.tableModel().selection().empty().get());
		assertTrue(departmentModel.editModel().editor().exists().get());
		departmentModel.editModel().editor().defaults();
		assertTrue(departmentModel.tableModel().selection().empty().get());
	}

	@Test
	public void test() {
		assertNotNull(departmentModel.editModel());
	}

	@Test
	public void detailModel() {
		departmentModel.detailModels().get((Class<? extends Model>) departmentModel.detailModels().get(Employee.TYPE).getClass());
		assertTrue(departmentModel.detailModels().contains(Employee.TYPE));
		Model detailModel = departmentModel.detailModels().get(Employee.TYPE);
		assertTrue(departmentModel.detailModels().contains(detailModel));
		assertTrue(departmentModel.detailModels().contains((Class<? extends Model>) departmentModel.detailModels().get(Employee.TYPE).getClass()));
		assertEquals(1, departmentModel.detailModels().get().size(), "Only one detail model should be in DepartmentModel");
		assertEquals(1, departmentModel.detailModels().linked().size());

		departmentModel.detailModels().get(Employee.TYPE);

		assertTrue(departmentModel.detailModels().linked().contains(departmentModel.detailModels().get(Employee.TYPE)));
		assertNotNull(departmentModel.detailModels().get(Employee.TYPE));
		if (!departmentModel.containsTableModel()) {
			return;
		}

		departmentModel.tableModel().items().refresh();
		departmentModel.detailModels().get(Employee.TYPE).tableModel().items().refresh();
		assertTrue(departmentModel.detailModels().get(Employee.TYPE).tableModel().items().visible().count() > 0);

		EntityConnection connection = departmentModel.connection();
		Entity department = connection.selectSingle(Department.NAME.equalTo("SALES"));

		departmentModel.tableModel().selection().item().set(department);

		List<Entity> salesEmployees = connection.select(Employee.DEPARTMENT_FK.equalTo(department));
		assertFalse(salesEmployees.isEmpty());
		departmentModel.tableModel().selection().item().set(department);
		Collection<Entity> employeesFromDetailModel =
						departmentModel.detailModels().get(Employee.TYPE).tableModel().items().get();
		assertTrue(salesEmployees.containsAll(employeesFromDetailModel), "Filtered list should contain all employees for department");
	}

	@Test
	public void addSameDetailModelTwice() {
		Model model = createDepartmentModelWithoutDetailModel();
		Model employeeModel = createEmployeeModel();
		assertThrows(IllegalArgumentException.class, () -> model.detailModels().add(employeeModel, employeeModel));
	}

	@Test
	public void addModelAsItsOwnDetailModel() {
		Model model = createDepartmentModelWithoutDetailModel();
		assertThrows(IllegalArgumentException.class, () -> model.detailModels().add(model));
	}

	@Test
	public void activateDeactivateDetailModel() {
		departmentModel.detailModels().link(departmentModel.detailModels().get(Employee.TYPE)).active().set(false);
		assertTrue(departmentModel.detailModels().linked().get().isEmpty());
		departmentModel.detailModels().link(departmentModel.detailModels().get(Employee.TYPE)).active().set(true);
		assertFalse(departmentModel.detailModels().linked().isEmpty());
		assertTrue(departmentModel.detailModels().linked().contains(departmentModel.detailModels().get(Employee.TYPE)));
	}

	@Test
	public void foreignKeyDetailModelLink() {
		if (!departmentModel.containsTableModel()) {
			return;
		}
		EntityConnection connection = connectionProvider.connection();
		Model employeeModel = departmentModel.detailModels().get(Employee.TYPE);
		ForeignKeyDetailModelLink<Model, EditModel, TableModel> detailModelLink = departmentModel.detailModels().link(employeeModel);

		TableModel deptTableModel = departmentModel.tableModel();
		EditModel deptEditModel = departmentModel.editModel();

		TableModel empTableModel = employeeModel.tableModel();
		EditModel empEditModel = employeeModel.editModel();
		Value<Entity> departmentEditModelValue = empEditModel.value(Employee.DEPARTMENT_FK);
		ConditionModel<Entity> deptCondition = empTableModel.queryModel()
						.conditions()
						.get(Employee.DEPARTMENT_FK);

		// setConditionOnInsert()
		connection.startTransaction();
		try {
			detailModelLink.setConditionOnInsert().set(true);

			deptEditModel.value(Department.ID).set(-10);
			deptEditModel.value(Department.NAME).set("New dept");
			Entity inserted = deptEditModel.insert();

			assertEquals(deptCondition.operands().in().get(), singleton(inserted));

			detailModelLink.setConditionOnInsert().set(false);
			deptCondition.clear();

			deptEditModel.value(Department.ID).set(-11);
			deptEditModel.value(Department.NAME).set("New dept2");

			deptEditModel.insert();
			assertTrue(deptCondition.operands().in().get().isEmpty());
		}
		finally {
			connection.rollbackTransaction();
		}

		// setValueOnInsert()
		connection.startTransaction();
		try {
			detailModelLink.setValueOnInsert().set(true);
			deptEditModel.value(Department.ID).set(-10);
			deptEditModel.value(Department.NAME).set("New dept");
			Entity inserted = deptEditModel.insert();

			assertEquals(departmentEditModelValue.get(), inserted);

			// but not when an existing entity is active
			empTableModel.queryModel().conditionRequired().set(false);
			empTableModel.items().refresh();
			empTableModel.selection().index().set(0);// select existing

			Entity currentDept = departmentEditModelValue.get();

			deptEditModel.value(Department.ID).set(-12);
			deptEditModel.value(Department.NAME).set("New dept3");
			deptEditModel.insert();

			assertSame(currentDept, departmentEditModelValue.get());

			detailModelLink.setValueOnInsert().set(false);
			empTableModel.selection().clear();
			departmentEditModelValue.clear();

			deptEditModel.value(Department.ID).set(-11);
			deptEditModel.value(Department.NAME).set("New dept2");

			deptEditModel.insert();
			assertTrue(departmentEditModelValue.isNull());


			detailModelLink.setValueOnInsert().set(true);

		}
		finally {
			connection.rollbackTransaction();
		}

		// clearValueOnEmptySelection()
		detailModelLink.clearValueOnEmptySelection().set(false);
		deptTableModel.items().refresh();
		deptTableModel.selection().index().set(0);
		Entity selected = deptTableModel.selection().item().get();
		assertSame(selected, departmentEditModelValue.get());
		deptTableModel.selection().clear();
		assertSame(selected, departmentEditModelValue.get());

		detailModelLink.clearValueOnEmptySelection().set(true);
		deptTableModel.selection().index().set(0);
		deptTableModel.selection().clear();
		assertTrue(departmentEditModelValue.isNull());

		// but not when an existing entity is active
		deptTableModel.selection().index().set(0);

		empTableModel.items().refresh();
		empTableModel.selection().index().set(0);// select existing

		deptTableModel.selection().clear();
		assertFalse(departmentEditModelValue.isNull());

		// clearConditionOnEmptySelection()
		detailModelLink.clearConditionOnEmptySelection().set(false);
		deptTableModel.items().refresh();
		deptTableModel.selection().indexes().set(asList(0, 1));
		List<Entity> selectedEntities = deptTableModel.selection().items().get();
		assertEquals(new HashSet<>(selectedEntities), deptCondition.operands().in().get());
		deptTableModel.selection().clear();
		assertEquals(new HashSet<>(selectedEntities), deptCondition.operands().in().get());

		detailModelLink.clearConditionOnEmptySelection().set(true);
		deptTableModel.selection().indexes().set(asList(2, 3));
		selectedEntities = deptTableModel.selection().items().get();
		assertEquals(new HashSet<>(selectedEntities), deptCondition.operands().in().get());
		deptTableModel.selection().clear();
		assertTrue(deptCondition.operands().in().get().isEmpty());
	}

	@Test
	public void searchByInsertedEntity() {
		if (!departmentModel.containsTableModel()) {
			return;
		}
		Model employeeModel = departmentModel.detailModels().get(Employee.TYPE);
		ForeignKeyDetailModelLink<Model, EditModel, TableModel> link = departmentModel.detailModels().link(employeeModel);
		link.setConditionOnInsert().set(true);
		assertTrue(link.setConditionOnInsert().get());
		EntityEditModel editModel = departmentModel.editModel();
		editModel.value(Department.ID).set(100);
		editModel.value(Department.NAME).set("Name");
		editModel.value(Department.LOCATION).set("Loc");
		Entity inserted = editModel.insert();
		Entity inValue = employeeModel.tableModel().queryModel().conditions()
						.get(Employee.DEPARTMENT_FK)
						.operands().in().iterator().next();
		assertEquals(inserted, inValue);
		editModel.delete();
	}

	@Test
	public void clearForeignKeyOnEmptySelection() {
		if (!departmentModel.containsTableModel()) {
			return;
		}
		Model employeeModel = departmentModel.detailModels().get(Employee.TYPE);
		EditModel employeeEditModel = employeeModel.editModel();

		ForeignKeyDetailModelLink<Model, EditModel, TableModel> link = departmentModel.detailModels().link(employeeModel);
		link.clearValueOnEmptySelection().set(false);

		Entity dept = employeeModel.connection().selectSingle(Department.ID.equalTo(10));

		departmentModel.tableModel().items().refresh();
		departmentModel.tableModel().selection().item().set(dept);
		assertEquals(dept, employeeEditModel.value(Employee.DEPARTMENT_FK).get());

		departmentModel.tableModel().selection().clear();
		assertEquals(dept, employeeEditModel.value(Employee.DEPARTMENT_FK).get());

		link.clearValueOnEmptySelection().set(true);

		departmentModel.tableModel().selection().item().set(dept);
		assertEquals(dept, employeeEditModel.value(Employee.DEPARTMENT_FK).get());

		departmentModel.tableModel().selection().clear();
		assertTrue(employeeEditModel.editor().isNull(Employee.DEPARTMENT_FK).get());

		link.clearValueOnEmptySelection().set(false);

		departmentModel.tableModel().selection().item().set(dept);
		assertEquals(dept, employeeEditModel.value(Employee.DEPARTMENT_FK).get());
	}

	@Test
	public void refreshOnSelection() {
		if (!departmentModel.containsTableModel()) {
			return;
		}
		Model employeeModel = departmentModel.detailModels().get(Employee.TYPE);
		TableModel employeeTableModel = employeeModel.tableModel();

		ForeignKeyDetailModelLink<Model, EditModel, TableModel> link = departmentModel.detailModels().link(employeeModel);
		link.refreshOnSelection().set(false);

		Entity dept = employeeModel.connection().selectSingle(Department.ID.equalTo(10));

		departmentModel.tableModel().items().refresh();
		departmentModel.tableModel().selection().item().set(dept);
		assertEquals(0, employeeTableModel.items().visible().count());

		link.refreshOnSelection().set(true);
		departmentModel.tableModel().selection().item().set(dept);
		assertNotEquals(0, employeeTableModel.items().visible().count());
	}

	@Test
	public void insertDifferentTypes() {
		if (!departmentModel.containsTableModel()) {
			return;
		}
		Entity dept = departmentModel.entities().builder(Department.TYPE)
						.with(Department.ID, -42)
						.with(Department.NAME, "Name")
						.with(Department.LOCATION, "Loc")
						.build();

		Entity emp = connectionProvider.connection().selectSingle(Employee.ID.equalTo(8)).clearPrimaryKey();
		emp.put(Employee.NAME, "NewName");

		Model model = createDepartmentModelWithoutDetailModel();
		model.editModel().insert(asList(dept, emp));
		assertTrue(model.tableModel().items().contains(dept));
		assertFalse(model.tableModel().items().contains(emp));

		model.editModel().delete(asList(dept, emp));

		assertFalse(model.tableModel().items().contains(dept));
	}

	protected final EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	/**
	 * @return a EntityModel based on the department entity
	 * @see Department#TYPE
	 */
	protected abstract Model createDepartmentModel();

	/**
	 * @return a EntityModel based on the department entity, without detail models
	 * @see Department#TYPE
	 */
	protected abstract Model createDepartmentModelWithoutDetailModel();

	/**
	 * @return a EntityModel based on the employee entity
	 * @see Employee#TYPE
	 */
	protected abstract Model createEmployeeModel();
}