/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.User;
import org.jminor.framework.db.DefaultEntityConnectionTest;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import java.util.Enumeration;

import static org.junit.Assert.*;

public final class DefaultEntityApplicationModelTest {

  @Test
  public void getDependencyTreeModel() {
    Chinook.init();
    final TreeModel model = DefaultEntityApplicationModel.getDependencyTreeModel(Chinook.DOMAIN_ID);
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
    final Enumeration tree = root.preorderEnumeration();
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.nextElement();
    assertNull(node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_ARTIST, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_ALBUM, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_TRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLISTTRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICELINE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_EMPLOYEE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_EMPLOYEE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_CUSTOMER, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICELINE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_GENRE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_TRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLISTTRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICELINE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_MEDIATYPE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_TRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLISTTRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICELINE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLIST, node.getUserObject());
  }

  @Test
  public void test() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(DefaultEntityConnectionTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        EmpDept.init();
      }
    };
    model.addEntityModels(new DeptModel(model.getConnectionProvider()));
    final EntityModel deptModel = model.getEntityModel(EmpDept.T_DEPARTMENT);
    assertNotNull(deptModel);
    deptModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().setQueryCriteriaRequired(false);
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
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(DefaultEntityConnectionTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {}
    };
    model.login(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getEntityModelByEntityIDNotFound() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(DefaultEntityConnectionTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        EmpDept.init();
      }
    };
    model.getEntityModel(EmpDept.T_DEPARTMENT);
  }

  @Test
  public void getEntityModelByEntityID() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(DefaultEntityConnectionTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        EmpDept.init();
      }
    };
    final DeptModel departmentModel = new DeptModel(model.getConnectionProvider());
    model.addEntityModels(departmentModel);
    assertEquals(departmentModel, model.getEntityModel(EmpDept.T_DEPARTMENT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getEntityModelByClassNotFound() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(DefaultEntityConnectionTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        EmpDept.init();
      }
    };
    model.getEntityModel(DeptModel.class);
  }

  @Test
  public void getEntityModelByClass() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(DefaultEntityConnectionTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        EmpDept.init();
      }
    };
    final DeptModel departmentModel = new DeptModel(model.getConnectionProvider());
    model.addEntityModels(departmentModel);
    assertEquals(departmentModel, model.getEntityModel(DeptModel.class));
  }

  @Test
  public void containsEntityModel() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(DefaultEntityConnectionTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        EmpDept.init();
      }
    };
    final DeptModel departmentModel = new DeptModel(model.getConnectionProvider());
    model.addEntityModels(departmentModel);

    assertTrue(model.containsEntityModel(EmpDept.T_DEPARTMENT));
    assertTrue(model.containsEntityModel(DeptModel.class));
    assertTrue(model.containsEntityModel(departmentModel));

    assertFalse(model.containsEntityModel(EmpDept.T_EMPLOYEE));
    assertFalse(model.containsEntityModel(departmentModel.getDetailModel(EmpDept.T_EMPLOYEE)));
  }

  @Test
  public void containsUnsavedData() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(DefaultEntityConnectionTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        EmpDept.init();
      }
    };
    final EntityModel deptModel = new DefaultEntityModel(EmpDept.T_DEPARTMENT, model.getConnectionProvider());
    final EntityModel empModel = new DefaultEntityModel(EmpDept.T_EMPLOYEE, model.getConnectionProvider());
    deptModel.addDetailModel(empModel);
    deptModel.addLinkedDetailModel(empModel);

    model.addEntityModel(deptModel);

    assertFalse(model.containsUnsavedData());

    model.refresh();

    deptModel.getTableModel().getSelectionModel().setSelectedIndex(0);
    empModel.getTableModel().getSelectionModel().setSelectedIndex(0);

    String name = (String) empModel.getEditModel().getValue(EmpDept.EMPLOYEE_NAME);
    empModel.getEditModel().setValue(EmpDept.EMPLOYEE_NAME, "Darri");
    assertTrue(model.containsUnsavedData());

    empModel.getEditModel().setValue(EmpDept.EMPLOYEE_NAME, name);
    assertFalse(model.containsUnsavedData());

    name = (String) deptModel.getEditModel().getValue(EmpDept.DEPARTMENT_NAME);
    deptModel.getEditModel().setValue(EmpDept.DEPARTMENT_NAME, "Darri");
    assertTrue(model.containsUnsavedData());

    deptModel.getEditModel().setValue(EmpDept.DEPARTMENT_NAME, name);
    assertFalse(model.containsUnsavedData());
  }

  private static class DeptModel extends DefaultEntityModel {
    private DeptModel(final EntityConnectionProvider connectionProvider) {
      super(EmpDept.T_DEPARTMENT, connectionProvider);
      addDetailModel(new DefaultEntityModel(EmpDept.T_EMPLOYEE, connectionProvider));
    }
  }
}
