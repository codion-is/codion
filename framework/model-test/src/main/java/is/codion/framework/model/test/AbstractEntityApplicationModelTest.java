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
    assertNotNull(model.entityModel(TestDomain.T_DEPARTMENT));
    assertEquals(1, model.entityModels().size());
    model.clear();
    assertEquals(UNIT_TEST_USER, model.user());

    assertThrows(IllegalArgumentException.class, () -> model.entityModel(TestDomain.T_EMP));
    if (!deptModel.containsTableModel()) {
      return;
    }
    deptModel.detailModel(TestDomain.T_EMP).tableModel().queryConditionRequiredState().set(false);
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
    assertThrows(IllegalArgumentException.class, () -> model.entityModel(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void entityModelByEntityType() {
    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    Model departmentModel = createDepartmentModel();
    model.addEntityModel(departmentModel);
    assertEquals(departmentModel, model.entityModel(TestDomain.T_DEPARTMENT));
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

    assertTrue(model.containsEntityModel(TestDomain.T_DEPARTMENT));
    assertTrue(model.containsEntityModel((Class<? extends Model>) departmentModel.getClass()));
    assertTrue(model.containsEntityModel(departmentModel));

    assertFalse(model.containsEntityModel(TestDomain.T_EMP));
    assertFalse(model.containsEntityModel(departmentModel.detailModel(TestDomain.T_EMP)));
  }

  @Test
  public void containsUnsavedData() {
    Model deptModel = createDepartmentModel();
    if (!deptModel.containsTableModel()) {
      return;
    }

    Model empModel = deptModel.detailModel(TestDomain.T_EMP);
    deptModel.addLinkedDetailModel(empModel);

    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    model.addEntityModel(deptModel);

    assertFalse(model.containsUnsavedData());

    model.refresh();

    deptModel.tableModel().selectionModel().setSelectedIndex(0);
    empModel.tableModel().selectionModel().setSelectedIndex(0);

    String name = empModel.editModel().get(TestDomain.EMP_NAME);
    empModel.editModel().put(TestDomain.EMP_NAME, "Darri");
    assertTrue(model.containsUnsavedData());

    empModel.editModel().put(TestDomain.EMP_NAME, name);
    assertFalse(model.containsUnsavedData());

    name = deptModel.editModel().get(TestDomain.DEPARTMENT_NAME);
    deptModel.editModel().put(TestDomain.DEPARTMENT_NAME, "Darri");
    assertTrue(model.containsUnsavedData());

    deptModel.editModel().put(TestDomain.DEPARTMENT_NAME, name);
    assertFalse(model.containsUnsavedData());
  }

  protected final EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  /**
   * @return a EntityModel based on the department entity
   * @see TestDomain#T_DEPARTMENT
   */
  protected abstract Model createDepartmentModel();
}
