/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public abstract class AbstractEntityApplicationModelTest<Model extends DefaultEntityModel<Model, EditModel, TableModel>,
        EditModel extends DefaultEntityEditModel, TableModel extends EntityTableModel<EditModel>> {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(UNIT_TEST_USER, Databases.getInstance());

  protected abstract Model createDepartmentModel();

  @BeforeClass
  public static void setUp() {
    TestDomain.init();
  }

  @Test
  public void test() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    model.addEntityModels(createDepartmentModel());
    final EntityModel deptModel = model.getEntityModel(TestDomain.T_DEPARTMENT);
    assertNotNull(deptModel);
    deptModel.getDetailModel(TestDomain.T_EMP).getTableModel().setQueryConditionRequired(false);
    assertEquals(1, model.getEntityModels().size());
    assertNotNull(deptModel);
    assertEquals(UNIT_TEST_USER, model.getUser());
    model.refresh();
    assertTrue(deptModel.getTableModel().getRowCount() > 0);
    model.logout();
    assertFalse(model.getConnectionProvider().isConnected());
    model.login(UNIT_TEST_USER);
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullConnectionProvider() {
    new DefaultEntityApplicationModel(null) {
      @Override
      protected void loadDomainModel() {}
    };
  }

  @Test(expected = NullPointerException.class)
  public void loginNullUser() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {}
    };
    model.login(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getEntityModelByEntityIDNotFound() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    model.getEntityModel(TestDomain.T_DEPARTMENT);
  }

  @Test
  public void getEntityModelByEntityID() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    final Model departmentModel = createDepartmentModel();
    model.addEntityModels(departmentModel);
    assertEquals(departmentModel, model.getEntityModel(TestDomain.T_DEPARTMENT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getEntityModelByClassNotFound() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    model.getEntityModel(EntityModel.class);
  }

  @Test
  public void getEntityModelByClass() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    final Model departmentModel = createDepartmentModel();
    model.addEntityModels(departmentModel);
    assertEquals(departmentModel, model.getEntityModel(departmentModel.getClass()));
  }

  @Test
  public void containsEntityModel() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
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
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    final Model deptModel = createDepartmentModel();
    final Model empModel = deptModel.getDetailModel(TestDomain.T_EMP);
    deptModel.addLinkedDetailModel(empModel);

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
}
