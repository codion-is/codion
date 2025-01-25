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

	@Test
	public void testUpdatePrimaryKey() {
		Model departmentModel = createDepartmentModel();
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
		Model departmentModel = createDepartmentModel();
		assertEquals(1, departmentModel.detailModels().linked().size());
		departmentModel.detailModels().link(departmentModel.detailModels().get(Employee.TYPE)).active().set(false);
		assertTrue(departmentModel.detailModels().linked().isEmpty());
		departmentModel.detailModels().link(departmentModel.detailModels().get(Employee.TYPE)).active().set(true);
	}

	@Test
	public void detailModelNotFound() {
		Model departmentModel = createDepartmentModel();
		assertThrows(IllegalArgumentException.class, () -> departmentModel.detailModels().get(Department.TYPE));
	}

	@Test
	public void clear() {
		Model departmentModel = createDepartmentModel();
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
		Model departmentModel = createDepartmentModel();
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
		Model departmentModel = createDepartmentModel();
		assertNotNull(departmentModel.editModel());
	}

	@Test
	public void detailModel() {
		Model departmentModel = createDepartmentModel();
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
		Model departmentModel = createDepartmentModel();
		departmentModel.detailModels().link(departmentModel.detailModels().get(Employee.TYPE)).active().set(false);
		assertTrue(departmentModel.detailModels().linked().get().isEmpty());
		departmentModel.detailModels().link(departmentModel.detailModels().get(Employee.TYPE)).active().set(true);
		assertFalse(departmentModel.detailModels().linked().isEmpty());
		assertTrue(departmentModel.detailModels().linked().contains(departmentModel.detailModels().get(Employee.TYPE)));
	}

	@Test
	public void setConditionOnInsert() {
		Model departmentModel = createDepartmentModelWithoutDetailModel();
		if (!departmentModel.containsTableModel()) {
			return;
		}
		Model employeeModel = createEmployeeModel();
		departmentModel.detailModels().add(ForeignKeyDetailModelLink.builder(employeeModel, Employee.DEPARTMENT_FK)
						.active(true)
						.clearValueOnEmptySelection(false)
						.clearConditionOnEmptySelection(false)
						.refreshOnSelection(false)
						.setConditionOnInsert(true)
						.setValueOnInsert(false)
						.build());

		EntityConnection connection = departmentModel.connection();
		connection.startTransaction();
		try {
			ConditionModel<Entity> deptCondition = employeeModel.tableModel().queryModel()
							.conditions()
							.get(Employee.DEPARTMENT_FK);

			departmentModel.editModel().value(Department.ID).set(-10);
			departmentModel.editModel().value(Department.NAME).set("New dept");
			Entity inserted = departmentModel.editModel().insert();

			assertEquals(deptCondition.operands().in().get(), singleton(inserted));
		}
		finally {
			connection.rollbackTransaction();
		}

		departmentModel = createDepartmentModelWithoutDetailModel();
		employeeModel = createEmployeeModel();
		departmentModel.detailModels().add(ForeignKeyDetailModelLink.builder(employeeModel, Employee.DEPARTMENT_FK)
						.active(true)
						.clearValueOnEmptySelection(false)
						.clearConditionOnEmptySelection(false)
						.refreshOnSelection(false)
						.setConditionOnInsert(false)
						.setValueOnInsert(false)
						.build());

		connection.startTransaction();
		try {
			ConditionModel<Entity> deptCondition = employeeModel.tableModel().queryModel()
							.conditions()
							.get(Employee.DEPARTMENT_FK);

			deptCondition.clear();

			departmentModel.editModel().value(Department.ID).set(-11);
			departmentModel.editModel().value(Department.NAME).set("New dept2");

			departmentModel.editModel().insert();
			assertTrue(deptCondition.operands().in().get().isEmpty());
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	public void setValueOnInsert() {
		Model departmentModel = createDepartmentModelWithoutDetailModel();
		if (!departmentModel.containsTableModel()) {
			return;
		}
		Model employeeModel = createEmployeeModel();
		departmentModel.detailModels().add(ForeignKeyDetailModelLink.builder(employeeModel, Employee.DEPARTMENT_FK)
						.active(true)
						.clearValueOnEmptySelection(false)
						.clearConditionOnEmptySelection(false)
						.refreshOnSelection(false)
						.setConditionOnInsert(false)
						.setValueOnInsert(true)
						.build());
		Value<Entity> departmentEditModelValue = employeeModel.editModel().value(Employee.DEPARTMENT_FK);

		EntityConnection connection = departmentModel.connection();
		connection.startTransaction();
		try {
			departmentModel.editModel().value(Department.ID).set(-10);
			departmentModel.editModel().value(Department.NAME).set("New dept");
			Entity inserted = departmentModel.editModel().insert();

			assertEquals(departmentEditModelValue.get(), inserted);

			// but not when an existing entity is active
			employeeModel.tableModel().queryModel().conditionRequired().set(false);
			employeeModel.tableModel().items().refresh();
			employeeModel.tableModel().selection().index().set(0);// select existing

			Entity currentDept = departmentEditModelValue.get();

			departmentModel.editModel().value(Department.ID).set(-12);
			departmentModel.editModel().value(Department.NAME).set("New dept3");
			departmentModel.editModel().insert();

			assertSame(currentDept, departmentEditModelValue.get());
		}
		finally {
			connection.rollbackTransaction();
		}

		departmentModel = createDepartmentModelWithoutDetailModel();
		employeeModel = createEmployeeModel();
		departmentModel.detailModels().add(ForeignKeyDetailModelLink.builder(employeeModel, Employee.DEPARTMENT_FK)
						.active(true)
						.clearValueOnEmptySelection(false)
						.clearConditionOnEmptySelection(false)
						.refreshOnSelection(false)
						.setConditionOnInsert(false)
						.setValueOnInsert(false)
						.build());
		departmentEditModelValue = employeeModel.editModel().value(Employee.DEPARTMENT_FK);

		connection.startTransaction();
		try {
			employeeModel.tableModel().selection().clear();
			departmentEditModelValue.clear();

			departmentModel.editModel().value(Department.ID).set(-11);
			departmentModel.editModel().value(Department.NAME).set("New dept2");

			departmentModel.editModel().insert();
			assertTrue(departmentEditModelValue.isNull());
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void clearValueOnEmptySelection() {
		Model departmentModel = createDepartmentModelWithoutDetailModel();
		if (!departmentModel.containsTableModel()) {
			return;
		}
		Model employeeModel = createEmployeeModel();
		departmentModel.detailModels().add(ForeignKeyDetailModelLink.builder(employeeModel, Employee.DEPARTMENT_FK)
						.active(true)
						.clearValueOnEmptySelection(true)
						.clearConditionOnEmptySelection(false)
						.refreshOnSelection(false)
						.setConditionOnInsert(false)
						.setValueOnInsert(false)
						.build());
		Value<Entity> departmentEditModelValue = employeeModel.editModel().value(Employee.DEPARTMENT_FK);

		departmentModel.tableModel().items().refresh();
		departmentModel.tableModel().selection().index().set(0);
		departmentModel.tableModel().selection().clear();
		assertTrue(departmentEditModelValue.isNull());

		// but not when an existing entity is active
		departmentModel.tableModel().selection().index().set(0);

		employeeModel.tableModel().items().refresh();
		employeeModel.tableModel().selection().index().set(0);// select existing

		departmentModel.tableModel().selection().clear();
		assertFalse(departmentEditModelValue.isNull());

		departmentModel = createDepartmentModelWithoutDetailModel();
		employeeModel = createEmployeeModel();
		departmentModel.detailModels().add(ForeignKeyDetailModelLink.builder(employeeModel, Employee.DEPARTMENT_FK)
						.active(true)
						.clearValueOnEmptySelection(false)
						.clearConditionOnEmptySelection(false)
						.refreshOnSelection(false)
						.setConditionOnInsert(false)
						.setValueOnInsert(false)
						.build());
		departmentEditModelValue = employeeModel.editModel().value(Employee.DEPARTMENT_FK);

		departmentModel.tableModel().items().refresh();
		departmentModel.tableModel().selection().index().set(0);
		Entity selected = departmentModel.tableModel().selection().item().get();
		assertSame(selected, departmentEditModelValue.get());
		departmentModel.tableModel().selection().clear();
		assertSame(selected, departmentEditModelValue.get());
	}

	@Test
	void clearConditionOnEmptySelection() {
		Model departmentModel = createDepartmentModelWithoutDetailModel();
		if (!departmentModel.containsTableModel()) {
			return;
		}
		Model employeeModel = createEmployeeModel();
		departmentModel.detailModels().add(ForeignKeyDetailModelLink.builder(employeeModel, Employee.DEPARTMENT_FK)
						.active(true)
						.clearValueOnEmptySelection(false)
						.clearConditionOnEmptySelection(true)
						.refreshOnSelection(false)
						.setConditionOnInsert(false)
						.setValueOnInsert(false)
						.build());
		ConditionModel<Entity> deptCondition = employeeModel.tableModel().queryModel()
							.conditions()
							.get(Employee.DEPARTMENT_FK);

		departmentModel.tableModel().items().refresh();
		departmentModel.tableModel().selection().indexes().set(asList(2, 3));
		Collection<Entity> selectedEntities = departmentModel.tableModel().selection().items().get();
		assertEquals(new HashSet<>(selectedEntities), deptCondition.operands().in().get());
		departmentModel.tableModel().selection().clear();
		assertTrue(deptCondition.operands().in().get().isEmpty());

		departmentModel = createDepartmentModelWithoutDetailModel();
		employeeModel = createEmployeeModel();
		departmentModel.detailModels().add(ForeignKeyDetailModelLink.builder(employeeModel, Employee.DEPARTMENT_FK)
						.active(true)
						.clearValueOnEmptySelection(false)
						.clearConditionOnEmptySelection(false)
						.refreshOnSelection(false)
						.setConditionOnInsert(false)
						.setValueOnInsert(false)
						.build());
		deptCondition = employeeModel.tableModel().queryModel()
							.conditions()
							.get(Employee.DEPARTMENT_FK);

		departmentModel.tableModel().items().refresh();
		departmentModel.tableModel().selection().indexes().set(asList(0, 1));
		selectedEntities = departmentModel.tableModel().selection().items().get();
		assertEquals(new HashSet<>(selectedEntities), deptCondition.operands().in().get());
		departmentModel.tableModel().selection().clear();
		assertEquals(new HashSet<>(selectedEntities), deptCondition.operands().in().get());
	}

	@Test
	public void insertDifferentTypes() {
		Model departmentModel = createDepartmentModel();
		if (!departmentModel.containsTableModel()) {
			return;
		}
		Entity dept = departmentModel.entities().builder(Department.TYPE)
						.with(Department.ID, -42)
						.with(Department.NAME, "Name")
						.with(Department.LOCATION, "Loc")
						.build();

		Entity emp = connectionProvider().connection().selectSingle(Employee.ID.equalTo(8)).clearPrimaryKey();
		emp.put(Employee.NAME, "NewName");

		Model model = createDepartmentModelWithoutDetailModel();
		model.editModel().insert(asList(dept, emp));
		assertTrue(model.tableModel().items().contains(dept));
		assertFalse(model.tableModel().items().contains(emp));

		model.editModel().delete(asList(dept, emp));

		assertFalse(model.tableModel().items().contains(dept));
	}

	protected final EntityConnectionProvider connectionProvider() {
		return CONNECTION_PROVIDER;
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