/*
 * Copyright (c) 2011 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
            .editModelClass(DepartmentEditModel.class).modelClass(SwingEntityModel.class));
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(Department.TYPE)
            .tableModelClass(DepartmentTableModel.class).modelClass(SwingEntityModel.class));

    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(Department.TYPE)
            .modelClass(SwingEntityModel.class).editModelClass(DepartmentEditModel.class));
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(Department.TYPE)
            .tableModelClass(DepartmentTableModel.class).editModelClass(DepartmentEditModel.class));
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(Department.TYPE)
            .editModelClass(DepartmentEditModel.class).tableModelClass(DepartmentTableModel.class));
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(Department.TYPE)
            .modelClass(SwingEntityModel.class).tableModelClass(DepartmentTableModel.class));
  }

  @Test
  void testDetailModelBuilder() {
    SwingEntityModel.Builder departmentModelBuilder = SwingEntityModel.builder(Department.TYPE)
            .tableModelClass(DepartmentTableModel.class);
    SwingEntityModelBuilder employeeModelBuilder = new SwingEntityModelBuilder(Employee.TYPE);

    departmentModelBuilder.detailModelBuilder(employeeModelBuilder);

    SwingEntityModel departmentModel = departmentModelBuilder.buildModel(CONNECTION_PROVIDER);
    assertTrue(departmentModel.editModel() instanceof DepartmentEditModel);
    assertTrue(departmentModel.tableModel() instanceof DepartmentTableModel);
    assertTrue(departmentModel.containsDetailModel(Employee.TYPE));
  }

  @Test
  void factories() {
    SwingEntityModel.Builder builder = SwingEntityModel.builder(Department.TYPE)
            .editModelFactory(DepartmentEditModel::new)
            .tableModelFactory(DepartmentTableModel::new);
    SwingEntityModel model = builder.buildModel(CONNECTION_PROVIDER);
    assertTrue(model.editModel() instanceof DepartmentEditModel);
    assertTrue(model.tableModel() instanceof DepartmentTableModel);

    builder = SwingEntityModel.builder(Department.TYPE)
            .modelFactory(DepartmentModel::new);

    model = builder.buildModel(CONNECTION_PROVIDER);
    assertTrue(model instanceof DepartmentModel);
    assertTrue(model.editModel() instanceof DepartmentEditModel);
    assertTrue(model.tableModel() instanceof DepartmentTableModel);
  }

  @Test
  void modelClasses() {
    SwingEntityModel.Builder builder = SwingEntityModel.builder(Department.TYPE)
            .tableModelClass(DepartmentTableModel.class);
    SwingEntityModel model = builder.buildModel(CONNECTION_PROVIDER);
    assertTrue(model.editModel() instanceof DepartmentEditModel);
    assertTrue(model.tableModel() instanceof DepartmentTableModel);

    builder = SwingEntityModel.builder(Department.TYPE)
            .modelClass(DepartmentModel.class);

    model = builder.buildModel(CONNECTION_PROVIDER);
    assertTrue(model instanceof DepartmentModel);
    assertTrue(model.editModel() instanceof DepartmentEditModel);
    assertTrue(model.tableModel() instanceof DepartmentTableModel);
  }

  @Test
  void onBuild() {
    State modelBuilt = State.state();
    State editModelBuilt = State.state();
    State tableModelBuilt = State.state();

    SwingEntityModel.Builder builder = SwingEntityModel.builder(Department.TYPE)
            .tableModelClass(DepartmentTableModel.class)
            .onBuildModel(swingEntityModel -> modelBuilt.set(true))
            .onBuildEditModel(swingEntityEditModel -> editModelBuilt.set(true))
            .onBuildTableModel(swingEntityTableModel -> tableModelBuilt.set(true));

    builder.buildModel(CONNECTION_PROVIDER);

    assertTrue(modelBuilt.get());
    assertFalse(editModelBuilt.get());
    assertTrue(tableModelBuilt.get());

    modelBuilt.set(false);
    tableModelBuilt.set(false);

    builder = SwingEntityModel.builder(Department.TYPE)
            .editModelClass(DepartmentEditModel.class)
            .onBuildModel(swingEntityModel -> modelBuilt.set(true))
            .onBuildEditModel(swingEntityEditModel -> editModelBuilt.set(true))
            .onBuildTableModel(swingEntityTableModel -> tableModelBuilt.set(true));

    builder.buildModel(CONNECTION_PROVIDER);

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
