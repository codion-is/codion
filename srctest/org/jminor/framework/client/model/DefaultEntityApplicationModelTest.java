/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.User;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.demos.chinook.domain.Chinook;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;

public final class DefaultEntityApplicationModelTest {

  @Test
  public void getDependencyTreeModel() {
    Chinook.init();
    final TreeModel model = DefaultEntityApplicationModel.getDependencyTreeModel(Chinook.DOMAIN_ID);
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
    final Enumeration tree = root.depthFirstEnumeration();
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICELINE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLISTTRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_TRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_MEDIATYPE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICELINE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_CUSTOMER, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_EMPLOYEE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_EMPLOYEE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICELINE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLISTTRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_TRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_GENRE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICELINE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLISTTRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_TRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_ALBUM, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_ARTIST, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLISTTRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLIST, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertNull(node.getUserObject());
  }

  @Test
  public void test() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(EntityConnectionImplTest.DB_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        EmpDept.init();
      }
    };
    model.addMainApplicationModels(new EmpModel(model.getConnectionProvider()));
    final EntityModel deptModel = model.getMainApplicationModel(EmpDept.T_DEPARTMENT);
    final EntityModel empModel = model.getMainApplicationModel(EmpModel.class);
    assertNotNull(empModel);
    deptModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().setQueryCriteriaRequired(false);
    assertEquals(2, model.getMainApplicationModels().size());
    assertNotNull(deptModel);
    assertEquals(User.UNIT_TEST_USER, model.getUser());
    model.refresh();
    assertTrue(deptModel.getTableModel().getRowCount() > 0);
    model.logout();
    assertFalse(model.getConnectionProvider().isConnected());
    model.login(User.UNIT_TEST_USER);
  }

  private static class EmpModel extends DefaultEntityModel {
    private EmpModel(final EntityConnectionProvider connectionProvider) {
      super(EmpDept.T_EMPLOYEE, connectionProvider);
    }
  }
}
