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
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.framework.model.test.TestDomain.EnumEntity;
import is.codion.framework.model.test.TestDomain.EnumEntity.EnumType;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class SwingEntityEditModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	private SwingEntityEditModel employeeEditModel;

	@BeforeEach
	void setUp() {
		employeeEditModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
	}

	@Test
	void comboBoxModel() {
		FilterComboBoxModel<String> model = employeeEditModel.comboBoxModel(Employee.JOB);
		assertNotNull(model);
		assertEquals(model, employeeEditModel.comboBoxModel(Employee.JOB));
		employeeEditModel.refreshComboBoxModels();
		employeeEditModel.clearComboBoxModels();
		assertTrue(employeeEditModel.comboBoxModel(Employee.JOB).items().cleared());
		employeeEditModel.refreshComboBoxModels();
		employeeEditModel.comboBoxModel(Employee.JOB).items().clear();
		assertTrue(employeeEditModel.comboBoxModel(Employee.JOB).items().cleared());
	}

	@Test
	void foreignKeyComboBoxModel() {
		EntityComboBoxModel model = employeeEditModel.foreignKeyComboBoxModel(Employee.DEPARTMENT_FK);
		assertNotNull(model);
		assertTrue(model.items().cleared());
		assertTrue(model.items().get().isEmpty());
		employeeEditModel.refreshComboBoxModels();
		assertFalse(model.items().cleared());
		assertFalse(model.items().get().isEmpty());
		employeeEditModel.clearComboBoxModels();
		assertTrue(model.items().cleared());
		assertTrue(model.items().get().isEmpty());
		assertSame(model, employeeEditModel.foreignKeyComboBoxModel(Employee.DEPARTMENT_FK));
	}

	@Test
	void createForeignKeyComboBoxModel() {
		EntityComboBoxModel model = employeeEditModel.createForeignKeyComboBoxModel(Employee.DEPARTMENT_FK);
		assertNotNull(model);
		assertTrue(model.items().cleared());
		assertTrue(model.items().get().isEmpty());
		ForeignKeyDefinition deptForeignKey = employeeEditModel.entities()
						.definition(Employee.TYPE).foreignKeys().definition(Employee.DEPARTMENT_FK);
		assertEquals(deptForeignKey.attribute().referencedType(), model.entityType());
		model.refresh();
		for (Entity department : model.items().get()) {
			assertTrue(department.contains(Department.ID));
			assertTrue(department.contains(Department.NAME));
			assertFalse(department.contains(Department.LOCATION));
		}
	}

	@Test
	void replace() {
		Entity blake = employeeEditModel.connection()
						.selectSingle(Employee.NAME.equalTo("BLAKE"));
		employeeEditModel.foreignKeyComboBoxModel(Employee.MGR_FK);
		employeeEditModel.refreshComboBoxModels();
		assertNotSame(employeeEditModel.foreignKeyComboBoxModel(Employee.MGR_FK)
						.find(blake.primaryKey()).orElse(null), blake);
		employeeEditModel.replace(Employee.MGR_FK, singletonList(blake));
		assertSame(employeeEditModel.foreignKeyComboBoxModel(Employee.MGR_FK)
						.find(blake.primaryKey()).orElse(null), blake);
	}

	@Test
	void initializeComboBoxModels() {
		employeeEditModel.initializeComboBoxModels(Employee.DEPARTMENT_FK, Employee.MGR_FK, Employee.JOB);
		assertFalse(employeeEditModel.comboBoxModel(Employee.JOB).items().cleared());
		assertFalse(employeeEditModel.foreignKeyComboBoxModel(Employee.DEPARTMENT_FK).items().cleared());
		assertFalse(employeeEditModel.foreignKeyComboBoxModel(Employee.MGR_FK).items().cleared());
	}

	@Test
	void enumComboBoxModel() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(EnumEntity.TYPE, CONNECTION_PROVIDER);
		FilterComboBoxModel<EnumType> comboBoxModel = editModel.comboBoxModel(EnumEntity.ENUM_TYPE);
		comboBoxModel.refresh();
		assertEquals(4, comboBoxModel.getSize());
		for (EnumType enumType : EnumType.values()) {
			assertTrue(comboBoxModel.items().contains(enumType));
		}
	}
}
