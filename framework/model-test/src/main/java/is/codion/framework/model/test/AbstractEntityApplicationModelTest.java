/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model.test;

import is.codion.common.db.database.DatabaseFactory;
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
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setUser(UNIT_TEST_USER).setDomainClassName(TestDomain.class.getName());

  private final EntityConnectionProvider connectionProvider;

  protected AbstractEntityApplicationModelTest() {
    this.connectionProvider = CONNECTION_PROVIDER;
  }

  @Test
  public void test() {
    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    Model deptModel = createDepartmentModel();
    model.addEntityModel(deptModel);
    assertNotNull(model.getEntityModel(TestDomain.T_DEPARTMENT));
    assertEquals(1, model.getEntityModels().size());
    model.clear();
    model.logout();
    assertFalse(model.getConnectionProvider().isConnected());
    model.login(UNIT_TEST_USER);
    assertEquals(UNIT_TEST_USER, model.getUser());

    assertThrows(IllegalArgumentException.class, () -> model.getEntityModel(TestDomain.T_EMP));
    if (!deptModel.containsTableModel()) {
      return;
    }
    deptModel.getDetailModel(TestDomain.T_EMP).getTableModel().getQueryConditionRequiredState().set(false);
    model.refresh();
    assertTrue(deptModel.getTableModel().getRowCount() > 0);
  }

  @Test
  public void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new DefaultEntityApplicationModel<>(null));
  }

  @Test
  public void loginNullUser() {
    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    assertThrows(NullPointerException.class, () -> model.login(null));
  }

  @Test
  public void getEntityModelByEntityTypeNotFound() {
    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    assertThrows(IllegalArgumentException.class, () -> model.getEntityModel(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void getEntityModelByEntityType() {
    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    Model departmentModel = createDepartmentModel();
    model.addEntityModel(departmentModel);
    assertEquals(departmentModel, model.getEntityModel(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void getEntityModelByClass() {
    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    Model departmentModel = createDepartmentModel();
    assertThrows(IllegalArgumentException.class, () -> model.getEntityModel((Class<? extends Model>) departmentModel.getClass()));
    model.addEntityModels(departmentModel);
    assertEquals(departmentModel, model.getEntityModel((Class<? extends Model>) departmentModel.getClass()));
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
    assertFalse(model.containsEntityModel(departmentModel.getDetailModel(TestDomain.T_EMP)));
  }

  @Test
  public void containsUnsavedData() {
    Model deptModel = createDepartmentModel();
    if (!deptModel.containsTableModel()) {
      return;
    }

    Model empModel = deptModel.getDetailModel(TestDomain.T_EMP);
    deptModel.addLinkedDetailModel(empModel);

    EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
    model.addEntityModel(deptModel);

    assertFalse(model.containsUnsavedData());

    model.refresh();

    deptModel.getTableModel().getSelectionModel().setSelectedIndex(0);
    empModel.getTableModel().getSelectionModel().setSelectedIndex(0);

    String name = empModel.getEditModel().get(TestDomain.EMP_NAME);
    empModel.getEditModel().put(TestDomain.EMP_NAME, "Darri");
    assertTrue(model.containsUnsavedData());

    empModel.getEditModel().put(TestDomain.EMP_NAME, name);
    assertFalse(model.containsUnsavedData());

    name = deptModel.getEditModel().get(TestDomain.DEPARTMENT_NAME);
    deptModel.getEditModel().put(TestDomain.DEPARTMENT_NAME, "Darri");
    assertTrue(model.containsUnsavedData());

    deptModel.getEditModel().put(TestDomain.DEPARTMENT_NAME, name);
    assertFalse(model.containsUnsavedData());
  }

  protected final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /**
   * @return a EntityModel based on the department entity
   * @see TestDomain#T_DEPARTMENT
   */
  protected abstract Model createDepartmentModel();
}
