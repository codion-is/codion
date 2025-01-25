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
package is.codion.swing.framework.model;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.ForeignKeyModelLink;
import is.codion.framework.model.test.AbstractEntityModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityModelTest
				extends AbstractEntityModelTest<SwingEntityEditModel, SwingEntityTableModel> {

	@Override
	protected SwingEntityModel createDepartmentModel() {
		SwingEntityModel departmentModel = new SwingEntityModel(Department.TYPE, connectionProvider());
		SwingEntityModel employeeModel = new SwingEntityModel(Employee.TYPE, departmentModel.connectionProvider());
		employeeModel.editModel().refreshComboBoxModels();
		departmentModel.detailModels().add(ForeignKeyModelLink.builder(employeeModel, Employee.DEPARTMENT_FK)
						.active(true)
						.build());
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
		SwingEntityModel departmentModel = createDepartmentModel();
		SwingEntityModel employeeModel = departmentModel.detailModels().get(Employee.TYPE);
		SwingEntityEditModel employeeEditModel = employeeModel.editModel();
		SwingEntityTableModel employeeTableModel = employeeModel.tableModel();

		EntityComboBoxModel comboBoxModel = employeeEditModel.comboBoxModel(Employee.MGR_FK);
		comboBoxModel.selection().item().link(employeeEditModel.value(Employee.MGR_FK));
		employeeTableModel.items().refresh();
		for (Entity employee : employeeTableModel.items().get()) {
			employeeTableModel.selection().item().set(employee);
			assertFalse(employeeEditModel.editor().modified().get());
		}
	}

	@Test
	public void testDetailModels() {
		SwingEntityModel departmentModel = createDepartmentModel();
		assertTrue(departmentModel.detailModels().contains(Employee.TYPE));
		assertFalse(departmentModel.detailModels().contains(Department.TYPE));
		assertFalse(departmentModel.detailModels().contains(EmpModel.class));
		SwingEntityModel employeeModel = departmentModel.detailModels().get(Employee.TYPE);
		assertNotNull(employeeModel);
		assertTrue(departmentModel.detailModels().active().contains(employeeModel));
		departmentModel.tableModel().items().refresh();
		SwingEntityEditModel employeeEditModel = employeeModel.editModel();
		EntityComboBoxModel departmentsComboBoxModel = employeeEditModel.comboBoxModel(Employee.DEPARTMENT_FK);
		departmentsComboBoxModel.items().refresh();
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
		SwingEntityModel departmentModel = createDepartmentModel();
		assertThrows(IllegalArgumentException.class, () -> departmentModel.detailModels().get(EmpModel.class));
	}

	@Test
	public void test() {
		SwingEntityModel departmentModel = createDepartmentModel();
		EntityConnection connection = departmentModel.connection();
		connection.startTransaction();
		try {
			departmentModel.tableModel().items().refresh();
			Entity department = connection.selectSingle(Department.NAME.equalTo("OPERATIONS"));
			departmentModel.tableModel().selection().item().set(department);
			SwingEntityModel employeeModel = departmentModel.detailModels().get(Employee.TYPE);
			EntityComboBoxModel deptComboBoxModel = employeeModel.editModel()
							.comboBoxModel(Employee.DEPARTMENT_FK);
			deptComboBoxModel.items().refresh();
			deptComboBoxModel.setSelectedItem(department);
			departmentModel.tableModel().deleteSelected();
			assertEquals(3, employeeModel.editModel().comboBoxModel(Employee.DEPARTMENT_FK).getSize());
			assertNotNull(employeeModel.editModel().comboBoxModel(Employee.DEPARTMENT_FK).selection().item().get());
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
}
