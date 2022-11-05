/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model.test;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.model.DefaultEntityApplicationModel;
import is.codion.framework.model.DefaultEntityEditModel;
import is.codion.framework.model.DefaultEntityModel;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A base class for testing {@link EntityApplicationModel} subclasses.
 * @param <Model> the {@link EntityModel} type
 * @param <EditModel> the {@link EntityEditModel} type
 * @param <TableModel> the {@link EntityTableModel} type
 */
public abstract class AbstractEntityApplicationModelTest<Model extends DefaultEntityModel<Model, EditModel, TableModel>,
        EditModel extends DefaultEntityEditModel, TableModel extends EntityTableModel<EditModel>> {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .user(UNIT_TEST_USER)
          .domainClassName(TestDomain.class.getName())
          .build();

  private final EntityConnectionProvider connectionProvider;

  protected AbstractEntityApplicationModelTest() {
    this.connectionProvider = CONNECTION_PROVIDER;
  }

  @Test
  public void test() {
    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    Model deptModel = createDepartmentModel();
    model.addEntityModel(deptModel);
    assertNotNull(model.entityModel(Department.TYPE));
    assertEquals(1, model.entityModels().size());
    assertEquals(UNIT_TEST_USER, model.user());

    assertThrows(IllegalArgumentException.class, () -> model.entityModel(Employee.TYPE));
    if (!deptModel.containsTableModel()) {
      return;
    }
    deptModel.detailModel(Employee.TYPE).tableModel().queryConditionRequiredState().set(false);
    model.refresh();
    assertTrue(deptModel.tableModel().getRowCount() > 0);
  }

  @Test
  public void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new DefaultEntityApplicationModel<>(null));
  }

  @Test
  public void entityModelByEntityTypeNotFound() {
    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    assertThrows(IllegalArgumentException.class, () -> model.entityModel(Department.TYPE));
  }

  @Test
  public void entityModelByEntityType() {
    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    Model departmentModel = createDepartmentModel();
    model.addEntityModel(departmentModel);
    assertEquals(departmentModel, model.entityModel(Department.TYPE));
  }

  @Test
  public void entityModelByClass() {
    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    Model departmentModel = createDepartmentModel();
    assertThrows(IllegalArgumentException.class, () -> model.entityModel((Class<? extends Model>) departmentModel.getClass()));
    model.addEntityModels(departmentModel);
    assertEquals(departmentModel, model.entityModel((Class<? extends Model>) departmentModel.getClass()));
  }

  @Test
  public void containsEntityModel() {
    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    Model departmentModel = createDepartmentModel();
    model.addEntityModel(departmentModel);

    assertTrue(model.containsEntityModel(Department.TYPE));
    assertTrue(model.containsEntityModel((Class<? extends Model>) departmentModel.getClass()));
    assertTrue(model.containsEntityModel(departmentModel));

    assertFalse(model.containsEntityModel(Employee.TYPE));
    assertFalse(model.containsEntityModel(departmentModel.detailModel(Employee.TYPE)));
  }

  @Test
  public void containsUnsavedData() {
    Model deptModel = createDepartmentModel();
    if (!deptModel.containsTableModel()) {
      return;
    }

    Model empModel = deptModel.detailModel(Employee.TYPE);
    deptModel.detailModelHandler(empModel).setActive(true);

    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    model.addEntityModel(deptModel);

    assertFalse(model.containsUnsavedData());

    model.refresh();

    deptModel.tableModel().selectionModel().setSelectedIndex(0);
    empModel.tableModel().selectionModel().setSelectedIndex(0);

    String name = empModel.editModel().get(Employee.NAME);
    empModel.editModel().put(Employee.NAME, "Darri");
    assertTrue(model.containsUnsavedData());

    empModel.editModel().put(Employee.NAME, name);
    assertFalse(model.containsUnsavedData());

    name = deptModel.editModel().get(Department.NAME);
    deptModel.editModel().put(Department.NAME, "Darri");
    assertTrue(model.containsUnsavedData());

    deptModel.editModel().put(Department.NAME, name);
    assertFalse(model.containsUnsavedData());
  }

  protected final EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  /**
   * @return a EntityModel based on the department entity
   * @see Department#TYPE
   */
  protected abstract Model createDepartmentModel();
}
