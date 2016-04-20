/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.model.User;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.model.DefaultEntityApplicationModel;
import org.jminor.framework.model.EntityModel;

import org.junit.Test;

import static org.junit.Assert.*;

public final class DefaultEntityApplicationModelTest {

  @Test
  public void test() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(EntityConnectionProvidersTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    model.addEntityModels(new DeptModel(model.getConnectionProvider()));
    final EntityModel deptModel = model.getEntityModel(TestDomain.T_DEPARTMENT);
    assertNotNull(deptModel);
    deptModel.getDetailModel(TestDomain.T_EMP).getTableModel().setQueryCriteriaRequired(false);
    assertEquals(1, model.getEntityModels().size());
    assertNotNull(deptModel);
    assertEquals(User.UNIT_TEST_USER, model.getUser());
    model.refresh();
    assertTrue(deptModel.getTableModel().getRowCount() > 0);
    model.logout();
    assertFalse(model.getConnectionProvider().isConnected());
    model.login(User.UNIT_TEST_USER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullConnectionProvider() {
    new DefaultEntityApplicationModel(null) {
      @Override
      protected void loadDomainModel() {}
    };
  }

  @Test(expected = IllegalArgumentException.class)
  public void loginNullUser() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(EntityConnectionProvidersTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {}
    };
    model.login(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getEntityModelByEntityIDNotFound() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(EntityConnectionProvidersTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    model.getEntityModel(TestDomain.T_DEPARTMENT);
  }

  @Test
  public void getEntityModelByEntityID() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(EntityConnectionProvidersTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    final DeptModel departmentModel = new DeptModel(model.getConnectionProvider());
    model.addEntityModels(departmentModel);
    assertEquals(departmentModel, model.getEntityModel(TestDomain.T_DEPARTMENT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getEntityModelByClassNotFound() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(EntityConnectionProvidersTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    model.getEntityModel(DeptModel.class);
  }

  @Test
  public void getEntityModelByClass() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(EntityConnectionProvidersTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    final DeptModel departmentModel = new DeptModel(model.getConnectionProvider());
    model.addEntityModels(departmentModel);
    assertEquals(departmentModel, model.getEntityModel(DeptModel.class));
  }

  @Test
  public void containsEntityModel() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(EntityConnectionProvidersTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    final DeptModel departmentModel = new DeptModel(model.getConnectionProvider());
    model.addEntityModels(departmentModel);

    assertTrue(model.containsEntityModel(TestDomain.T_DEPARTMENT));
    assertTrue(model.containsEntityModel(DeptModel.class));
    assertTrue(model.containsEntityModel(departmentModel));

    assertFalse(model.containsEntityModel(TestDomain.T_EMP));
    assertFalse(model.containsEntityModel(departmentModel.getDetailModel(TestDomain.T_EMP)));
  }

  @Test
  public void containsUnsavedData() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(EntityConnectionProvidersTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    final EntityModel deptModel = new SwingEntityModel(TestDomain.T_DEPARTMENT, model.getConnectionProvider());
    final EntityModel empModel = new SwingEntityModel(TestDomain.T_EMP, model.getConnectionProvider());
    deptModel.addDetailModel(empModel);
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

  private static class DeptModel extends SwingEntityModel {
    private DeptModel(final EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_DEPARTMENT, connectionProvider);
      addDetailModel(new SwingEntityModel(TestDomain.T_EMP, connectionProvider));
    }
  }
}
