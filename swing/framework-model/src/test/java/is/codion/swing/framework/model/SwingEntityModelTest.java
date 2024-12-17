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
package is.codion.swing.framework.model;

import is.codion.common.value.AbstractValue;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.AbstractEntityModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityModelTest
				extends AbstractEntityModelTest<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

	@Override
	protected SwingEntityModel createDepartmentModel() {
		SwingEntityModel departmentModel = new SwingEntityModel(Department.TYPE, connectionProvider());
		SwingEntityModel employeeModel = new SwingEntityModel(Employee.TYPE, departmentModel.connectionProvider());
		employeeModel.editModel().refreshComboBoxModels();
		departmentModel.addDetailModel(employeeModel, Employee.DEPARTMENT_FK).active().set(true);
		employeeModel.tableModel().queryModel().conditionRequired().set(false);

		return departmentModel;
	}

	@Override
	protected SwingEntityModel createDepartmentModelWithoutDetailModel() {
		return new SwingEntityModel(Department.TYPE, connectionProvider());
	}

	@Override
	protected SwingEntityModel createEmployeeModel() {
		return new SwingEntityModel(Employee.TYPE, connectionProvider());
	}

	@Test
	void isModified() {
		//here we're basically testing for the entity in the edit model being modified after
		//being set when selected in the table model, this usually happens when combo box models
		//are being filtered on attribute value change, see EmployeeEditModel.bindEvents()
		SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
		SwingEntityEditModel employeeEditModel = employeeModel.editModel();
		SwingEntityTableModel employeeTableModel = employeeModel.tableModel();

		EntityComboBoxModel comboBoxModel = employeeEditModel.foreignKeyComboBoxModel(Employee.MGR_FK);
		new EntityComboBoxModelValue(comboBoxModel).link(employeeEditModel.value(Employee.MGR_FK));
		employeeTableModel.refresh();
		for (Entity employee : employeeTableModel.items().get()) {
			employeeTableModel.selection().item().set(employee);
			assertFalse(employeeEditModel.entity().modified().get());
		}
	}

	@Test
	public void testDetailModels() {
		assertTrue(departmentModel.containsDetailModel(Employee.TYPE));
		assertFalse(departmentModel.containsDetailModel(Department.TYPE));
		assertFalse(departmentModel.containsDetailModel(EmpModel.class));
		SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
		assertNotNull(employeeModel);
		assertTrue(departmentModel.linkedDetailModels().contains(employeeModel));
		departmentModel.tableModel().refresh();
		SwingEntityEditModel employeeEditModel = employeeModel.editModel();
		EntityComboBoxModel departmentsComboBoxModel = employeeEditModel.foreignKeyComboBoxModel(Employee.DEPARTMENT_FK);
		departmentsComboBoxModel.refresh();
		Entity.Key primaryKey = connectionProvider().entities().primaryKey(Department.TYPE, 40);//operations, no employees
		departmentModel.tableModel().select(Collections.singletonList(primaryKey));
		Entity operations = departmentModel.tableModel().selection().item().getOrThrow();
		EntityConnection connection = departmentModel.connection();
		connection.startTransaction();
		departmentModel.editModel().delete();
		assertFalse(departmentsComboBoxModel.items().contains(operations));
		departmentModel.editModel().value(Department.ID).set(99);
		departmentModel.editModel().value(Department.NAME).set("nameit");
		Entity inserted = departmentModel.editModel().insert();
		assertTrue(departmentsComboBoxModel.items().contains(inserted));
		departmentModel.tableModel().selection().item().set(inserted);
		departmentModel.editModel().value(Department.NAME).set("nameitagain");
		departmentModel.editModel().update();
		assertEquals("nameitagain", departmentsComboBoxModel.find(inserted.primaryKey()).orElse(null).get(Department.NAME));

		departmentModel.tableModel().select(Collections.singletonList(primaryKey.copy().with(Department.ID, 20).build()));
		departmentModel.editModel().value(Department.NAME).set("NewName");
		departmentModel.editModel().update();

		for (Entity employee : employeeModel.tableModel().items().get()) {
			Entity dept = employee.entity(Employee.DEPARTMENT_FK);
			assertEquals("NewName", dept.get(Department.NAME));
		}
		connection.rollbackTransaction();
	}

	@Test
	void getDetailModelNonExisting() {
		assertThrows(IllegalArgumentException.class, () -> departmentModel.detailModel(EmpModel.class));
	}

	@Test
	public void test() {
		super.test();
		EntityConnection connection = departmentModel.connection();
		connection.startTransaction();
		try {
			departmentModel.tableModel().refresh();
			Entity department = connection.selectSingle(Department.NAME.equalTo("OPERATIONS"));
			departmentModel.tableModel().selection().item().set(department);
			SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
			EntityComboBoxModel deptComboBoxModel = employeeModel.editModel()
							.foreignKeyComboBoxModel(Employee.DEPARTMENT_FK);
			deptComboBoxModel.refresh();
			deptComboBoxModel.setSelectedItem(department);
			departmentModel.tableModel().deleteSelected();
			assertEquals(3, employeeModel.editModel().foreignKeyComboBoxModel(Employee.DEPARTMENT_FK).getSize());
			assertNotNull(employeeModel.editModel().foreignKeyComboBoxModel(Employee.DEPARTMENT_FK).selection().item().get());
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void constructor() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Department.TYPE, connectionProvider());
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Department.TYPE, connectionProvider());

		new SwingEntityModel(editModel);
		new SwingEntityModel(tableModel);

		tableModel = new SwingEntityTableModel(Department.TYPE, connectionProvider());
		assertNotEquals(editModel, new SwingEntityModel(tableModel).editModel());

		tableModel = new SwingEntityTableModel(editModel);
		assertEquals(editModel, new SwingEntityModel(tableModel).editModel());
	}

	@Test
	void constructorNullEntityType() {
		assertThrows(NullPointerException.class, () -> new SwingEntityModel(null, connectionProvider()));
	}

	@Test
	void constructorNullConnectionProvider() {
		assertThrows(NullPointerException.class, () -> new SwingEntityModel(Employee.TYPE, null));
	}

	public static class EmpModel extends SwingEntityModel {
		public EmpModel(EntityConnectionProvider connectionProvider) {
			super(Employee.TYPE, connectionProvider);
		}
	}

	private static final class EntityComboBoxModelValue extends AbstractValue<Entity> {

		private final EntityComboBoxModel comboBoxModel;

		public EntityComboBoxModelValue(EntityComboBoxModel comboBoxModel) {
			this.comboBoxModel = comboBoxModel;
			comboBoxModel.selection().item().addListener(this::notifyListeners);
		}

		@Override
		protected void setValue(Entity value) {
			comboBoxModel.selection().item().set(value);
		}

		@Override
		protected Entity getValue() {
			return comboBoxModel.selection().item().get();
		}
	}
}
