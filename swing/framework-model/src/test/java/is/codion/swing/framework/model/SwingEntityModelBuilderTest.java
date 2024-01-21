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
 * Copyright (c) 2011 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityModelBuilderTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void setModelClass() {
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(Department.TYPE)
            .editModel(DepartmentEditModel.class).model(SwingEntityModel.class));
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(Department.TYPE)
            .tableModel(DepartmentTableModel.class).model(SwingEntityModel.class));

    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(Department.TYPE)
            .model(SwingEntityModel.class).editModel(DepartmentEditModel.class));
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(Department.TYPE)
            .tableModel(DepartmentTableModel.class).editModel(DepartmentEditModel.class));
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(Department.TYPE)
            .editModel(DepartmentEditModel.class).tableModel(DepartmentTableModel.class));
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(Department.TYPE)
            .model(SwingEntityModel.class).tableModel(DepartmentTableModel.class));
  }

  @Test
  void testDetailModelBuilder() {
    SwingEntityModel.Builder departmentModelBuilder = SwingEntityModel.builder(Department.TYPE)
            .tableModel(DepartmentTableModel.class);
    SwingEntityModelBuilder employeeModelBuilder = new SwingEntityModelBuilder(Employee.TYPE);

    departmentModelBuilder.detailModel(employeeModelBuilder);

    SwingEntityModel departmentModel = departmentModelBuilder.build(CONNECTION_PROVIDER);
    assertInstanceOf(DepartmentEditModel.class, departmentModel.editModel());
    assertInstanceOf(DepartmentTableModel.class, departmentModel.tableModel());
    assertTrue(departmentModel.containsDetailModel(Employee.TYPE));
  }

  @Test
  void factories() {
    SwingEntityModel.Builder builder = SwingEntityModel.builder(Department.TYPE)
            .editModel(DepartmentEditModel::new)
            .tableModel(DepartmentTableModel::new);
    SwingEntityModel model = builder.build(CONNECTION_PROVIDER);
    assertInstanceOf(DepartmentEditModel.class, model.editModel());
    assertInstanceOf(DepartmentTableModel.class, model.tableModel());

    builder = SwingEntityModel.builder(Department.TYPE)
            .model(DepartmentModel::new);

    model = builder.build(CONNECTION_PROVIDER);
    assertInstanceOf(DepartmentModel.class, model);
    assertInstanceOf(DepartmentEditModel.class, model.editModel());
    assertInstanceOf(DepartmentTableModel.class, model.tableModel());
  }

  @Test
  void modelClasses() {
    SwingEntityModel.Builder builder = SwingEntityModel.builder(Department.TYPE)
            .tableModel(DepartmentTableModel.class);
    SwingEntityModel model = builder.build(CONNECTION_PROVIDER);
    assertInstanceOf(DepartmentEditModel.class, model.editModel());
    assertInstanceOf(DepartmentTableModel.class, model.tableModel());

    builder = SwingEntityModel.builder(Department.TYPE)
            .model(DepartmentModel.class);

    model = builder.build(CONNECTION_PROVIDER);
    assertInstanceOf(DepartmentModel.class, model);
    assertInstanceOf(DepartmentEditModel.class, model.editModel());
    assertInstanceOf(DepartmentTableModel.class, model.tableModel());
  }

  @Test
  void onBuild() {
    State modelBuilt = State.state();
    State editModelBuilt = State.state();
    State tableModelBuilt = State.state();

    SwingEntityModel.Builder builder = SwingEntityModel.builder(Department.TYPE)
            .tableModel(DepartmentTableModel.class)
            .onBuildModel(swingEntityModel -> modelBuilt.set(true))
            .onBuildEditModel(swingEntityEditModel -> editModelBuilt.set(true))
            .onBuildTableModel(swingEntityTableModel -> tableModelBuilt.set(true));

    builder.build(CONNECTION_PROVIDER);

    assertTrue(modelBuilt.get());
    assertFalse(editModelBuilt.get());
    assertTrue(tableModelBuilt.get());

    modelBuilt.set(false);
    tableModelBuilt.set(false);

    builder = SwingEntityModel.builder(Department.TYPE)
            .editModel(DepartmentEditModel.class)
            .onBuildModel(swingEntityModel -> modelBuilt.set(true))
            .onBuildEditModel(swingEntityEditModel -> editModelBuilt.set(true))
            .onBuildTableModel(swingEntityTableModel -> tableModelBuilt.set(true));

    builder.build(CONNECTION_PROVIDER);

    assertTrue(modelBuilt.get());
    assertTrue(editModelBuilt.get());
    assertTrue(tableModelBuilt.get());
  }

  static final class DepartmentModel extends SwingEntityModel {

    public DepartmentModel(EntityConnectionProvider connectionProvider) {
      super(new DepartmentTableModel(connectionProvider));
    }
  }

  static final class DepartmentEditModel extends SwingEntityEditModel {

    public DepartmentEditModel(EntityConnectionProvider connectionProvider) {
      super(Department.TYPE, connectionProvider);
    }
  }

  static final class DepartmentTableModel extends SwingEntityTableModel {

    public DepartmentTableModel(EntityConnectionProvider connectionProvider) {
      super(new DepartmentEditModel(connectionProvider));
    }
  }
}
