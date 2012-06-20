/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.User;
import org.jminor.framework.db.EntityConnectionImplTest;
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
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(EntityConnectionImplTest.CONNECTION_PROVIDER) {
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
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(EntityConnectionImplTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {}
    };
    model.login(null);
  }

  private static class DeptModel extends DefaultEntityModel {
    private DeptModel(final EntityConnectionProvider connectionProvider) {
      super(EmpDept.T_DEPARTMENT, connectionProvider);
      addDetailModel(new DefaultEntityModel(EmpDept.T_EMPLOYEE, connectionProvider));
    }
  }
}
