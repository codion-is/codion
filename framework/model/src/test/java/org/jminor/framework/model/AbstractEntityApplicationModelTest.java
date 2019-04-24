/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Domain;

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

  protected static final Domain DOMAIN = new TestDomain();

  protected static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  protected static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setUser(UNIT_TEST_USER).setDomainClassName(TestDomain.class.getName());

  @Test
  public void test() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER);
    final DefaultEntityModel deptModel = createDepartmentModel();
    model.addEntityModels(deptModel);
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
    deptModel.getDetailModel(TestDomain.T_EMP).getTableModel().getQueryConditionRequiredState().setActive(false);
    model.refresh();
    assertTrue(deptModel.getTableModel().getRowCount() > 0);
  }

  @Test
  public void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new DefaultEntityApplicationModel(null));
  }

  @Test
  public void loginNullUser() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER);
    assertThrows(NullPointerException.class, () -> model.login(null));
  }

  @Test
  public void getEntityModelByEntityIDNotFound() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER);
    assertThrows(IllegalArgumentException.class, () -> model.getEntityModel(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void getEntityModelByEntityId() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER);
    final Model departmentModel = createDepartmentModel();
    model.addEntityModels(departmentModel);
    assertEquals(departmentModel, model.getEntityModel(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void getEntityModelByClassNotFound() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER);
    assertThrows(IllegalArgumentException.class, () -> model.getEntityModel(EntityModel.class));
  }

  @Test
  public void getEntityModelByClass() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER);
    final Model departmentModel = createDepartmentModel();
    model.addEntityModels(departmentModel);
    assertEquals(departmentModel, model.getEntityModel(departmentModel.getClass()));
  }

  @Test
  public void containsEntityModel() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER);
    final Model departmentModel = createDepartmentModel();
    model.addEntityModels(departmentModel);

    assertTrue(model.containsEntityModel(TestDomain.T_DEPARTMENT));
    assertTrue(model.containsEntityModel(departmentModel.getClass()));
    assertTrue(model.containsEntityModel(departmentModel));

    assertFalse(model.containsEntityModel(TestDomain.T_EMP));
    assertFalse(model.containsEntityModel(departmentModel.getDetailModel(TestDomain.T_EMP)));
  }

  @Test
  public void containsUnsavedData() {
    final Model deptModel = createDepartmentModel();
    if (!deptModel.containsTableModel()) {
      return;
    }

    final Model empModel = deptModel.getDetailModel(TestDomain.T_EMP);
    deptModel.addLinkedDetailModel(empModel);

    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER);
    model.addEntityModel(deptModel);

    assertFalse(model.containsUnsavedData());

    model.refresh();

    deptModel.getTableModel().getSelectionModel().setSelectedIndex(0);
    empModel.getTableModel().getSelectionModel().setSelectedIndex(0);

    String name = (String) empModel.getEditModel().getValue(TestDomain.EMP_NAME);
    empModel.getEditModel().setValue(TestDomain.EMP_NAME, "Darri");
    assertTrue(model.containsUnsavedData());

    empModel.getEditModel().setValue(TestDomain.EMP_NAME, name);
    assertFalse(model.containsUnsavedData());

    name = (String) deptModel.getEditModel().getValue(TestDomain.DEPARTMENT_NAME);
    deptModel.getEditModel().setValue(TestDomain.DEPARTMENT_NAME, "Darri");
    assertTrue(model.containsUnsavedData());

    deptModel.getEditModel().setValue(TestDomain.DEPARTMENT_NAME, name);
    assertFalse(model.containsUnsavedData());
  }

  /**
   * @return a EntityModel based on the department entity
   * @see TestDomain#T_DEPARTMENT
   */
  protected abstract Model createDepartmentModel();
}
