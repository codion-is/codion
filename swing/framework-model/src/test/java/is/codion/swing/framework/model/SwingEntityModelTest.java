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
		employeeModel.tableModel().conditionRequired().set(false);

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
		for (Entity employee : employeeTableModel.items()) {
			employeeTableModel.selectionModel().setSelectedItem(employee);
			assertFalse(employeeEditModel.modified().get());
		}
	}

	@Test
	public void testDetailModels() throws Exception {
		assertTrue(departmentModel.containsDetailModel(Employee.TYPE));
		assertFalse(departmentModel.containsDetailModel(Department.TYPE));
		assertFalse(departmentModel.containsDetailModel(EmpModel.class));
		SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
		assertNotNull(employeeModel);
		assertTrue(departmentModel.activeDetailModels().contains(employeeModel));
		departmentModel.tableModel().refresh();
		SwingEntityEditModel employeeEditModel = employeeModel.editModel();
		EntityComboBoxModel departmentsComboBoxModel = employeeEditModel.foreignKeyComboBoxModel(Employee.DEPARTMENT_FK);
		departmentsComboBoxModel.refresh();
		Entity.Key primaryKey = connectionProvider().entities().primaryKey(Department.TYPE, 40);//operations, no employees
		departmentModel.tableModel().select(Collections.singletonList(primaryKey));
		Entity operations = departmentModel.tableModel().selectionModel().getSelectedItem();
		EntityConnection connection = departmentModel.connection();
		connection.beginTransaction();
		try {
			departmentModel.editModel().delete();
			assertFalse(departmentsComboBoxModel.containsItem(operations));
			departmentModel.editModel().put(Department.ID, 99);
			departmentModel.editModel().put(Department.NAME, "nameit");
			Entity inserted = departmentModel.editModel().insert();
			assertTrue(departmentsComboBoxModel.containsItem(inserted));
			departmentModel.tableModel().selectionModel().setSelectedItem(inserted);
			departmentModel.editModel().put(Department.NAME, "nameitagain");
			departmentModel.editModel().update();
			assertEquals("nameitagain", departmentsComboBoxModel.find(inserted.primaryKey()).orElse(null).get(Department.NAME));

			departmentModel.tableModel().select(Collections.singletonList(primaryKey.copyBuilder().with(Department.ID, 20).build()));
			departmentModel.editModel().put(Department.NAME, "NewName");
			departmentModel.editModel().update();

			for (Entity employee : employeeModel.tableModel().items()) {
				Entity dept = employee.referencedEntity(Employee.DEPARTMENT_FK);
				assertEquals("NewName", dept.get(Department.NAME));
			}
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void getDetailModelNonExisting() {
		assertThrows(IllegalArgumentException.class, () -> departmentModel.detailModel(EmpModel.class));
	}

	@Test
	public void test() throws Exception {
		super.test();
		EntityConnection connection = departmentModel.connection();
		connection.beginTransaction();
		try {
			departmentModel.tableModel().refresh();
			Entity department = connection.selectSingle(Department.NAME.equalTo("OPERATIONS"));
			departmentModel.tableModel().selectionModel().setSelectedItem(department);
			SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
			EntityComboBoxModel deptComboBoxModel = employeeModel.editModel()
							.foreignKeyComboBoxModel(Employee.DEPARTMENT_FK);
			deptComboBoxModel.refresh();
			deptComboBoxModel.setSelectedItem(department);
			departmentModel.tableModel().deleteSelected();
			assertEquals(3, employeeModel.editModel().foreignKeyComboBoxModel(Employee.DEPARTMENT_FK).getSize());
			assertNotNull(employeeModel.editModel().foreignKeyComboBoxModel(Employee.DEPARTMENT_FK).selectedValue());
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
			comboBoxModel.addSelectionListener(selected -> notifyListeners());
		}

		@Override
		protected void setValue(Entity value) {
			comboBoxModel.setSelectedItem(value);
		}

		@Override
		public Entity get() {
			return comboBoxModel.selectedValue();
		}
	}
}
