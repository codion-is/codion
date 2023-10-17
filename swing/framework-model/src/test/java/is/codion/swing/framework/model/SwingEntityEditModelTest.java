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
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
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
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;
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
    FilteredComboBoxModel<String> model = employeeEditModel.comboBoxModel(Employee.JOB);
    model.includeNull().set(true);
    model.nullItem().set("null");
    assertNotNull(model);
    assertTrue(employeeEditModel.containsComboBoxModel(Employee.JOB));
    assertEquals(model, employeeEditModel.comboBoxModel(Employee.JOB));
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clearComboBoxModels();
    assertTrue(employeeEditModel.comboBoxModel(Employee.JOB).cleared());
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.comboBoxModel(Employee.JOB).clear();
    assertTrue(employeeEditModel.comboBoxModel(Employee.JOB).cleared());
  }

  @Test
  void foreignKeyComboBoxModel() {
    assertFalse(employeeEditModel.containsComboBoxModel(Employee.DEPARTMENT_FK));
    EntityComboBoxModel model = employeeEditModel.foreignKeyComboBoxModel(Employee.DEPARTMENT_FK);
    assertNotNull(model);
    assertTrue(model.cleared());
    assertTrue(model.items().isEmpty());
    employeeEditModel.refreshComboBoxModels();
    assertFalse(model.cleared());
    assertFalse(model.items().isEmpty());
    employeeEditModel.clearComboBoxModels();
    assertTrue(model.cleared());
    assertTrue(model.items().isEmpty());
  }

  @Test
  void createForeignKeyComboBoxModel() {
    EntityComboBoxModel model = employeeEditModel.createForeignKeyComboBoxModel(Employee.DEPARTMENT_FK);
    assertNotNull(model);
    assertTrue(model.cleared());
    assertTrue(model.items().isEmpty());
    ForeignKeyDefinition deptForeignKey = employeeEditModel.entities()
            .definition(Employee.TYPE).foreignKeys().definition(Employee.DEPARTMENT_FK);
    assertEquals(deptForeignKey.attribute().referencedType(), model.entityType());
    model.refresh();
    for (Entity department : model.items()) {
      assertTrue(department.contains(Department.ID));
      assertTrue(department.contains(Department.NAME));
      assertFalse(department.contains(Department.LOCATION));
    }
  }

  @Test
  void replace() throws DatabaseException {
    Entity blake = employeeEditModel.connectionProvider().connection()
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
    assertFalse(employeeEditModel.comboBoxModel(Employee.JOB).cleared());
    assertFalse(employeeEditModel.foreignKeyComboBoxModel(Employee.DEPARTMENT_FK).cleared());
    assertFalse(employeeEditModel.foreignKeyComboBoxModel(Employee.MGR_FK).cleared());
  }

  @Test
  void enumComboBoxModel() {
    SwingEntityEditModel editModel = new SwingEntityEditModel(EnumEntity.TYPE, CONNECTION_PROVIDER);
    FilteredComboBoxModel<EnumType> comboBoxModel = editModel.comboBoxModel(EnumEntity.ENUM_TYPE);
    comboBoxModel.refresh();
    assertEquals(4, comboBoxModel.getSize());
    for (EnumType enumType : EnumType.values()) {
      assertTrue(comboBoxModel.containsItem(enumType));
    }
  }
}
