/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.User;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.model.EntityApplicationModel;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.model.SwingEntityModel;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.junit.Assert.*;

public class EntityApplicationPanelTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  @BeforeClass
  public static void setUp() {
    TestDomain.init();
  }

  @After
  public void tearDown() {
    Thread.setDefaultUncaughtExceptionHandler(null);
  }

  @Test
  public void getDependencyTreeModel() {
    final TreeModel model = EntityApplicationPanel.getDependencyTreeModel(TestDomain.SCOTT_DOMAIN_ID);
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
    final Enumeration tree = root.preorderEnumeration();
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.nextElement();
    assertNull(node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(TestDomain.T_DEPARTMENT, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(TestDomain.T_EMP, node.getUserObject());
  }

  @Test
  public void test() throws Exception {
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
    EntityApplicationModel.SAVE_DEFAULT_USERNAME.set(false);
    final EntityApplicationPanel<SwingEntityApplicationModel> panel = new EntityApplicationPanel<SwingEntityApplicationModel>() {
      @Override
      protected List<EntityPanel> initializeEntityPanels(final SwingEntityApplicationModel applicationModel) {
        return Collections.singletonList(new EntityPanel(applicationModel.getEntityModel(TestDomain.T_EMP)));
      }
      @Override
      protected SwingEntityApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
        final SwingEntityApplicationModel model = new SwingEntityApplicationModel(connectionProvider) {
          @Override
          protected void loadDomainModel() {
            TestDomain.init();
          }
        };

        model.addEntityModel(new SwingEntityModel(TestDomain.T_EMP, connectionProvider));

        return model;
      }
    };
    panel.setLoginRequired(false);
    panel.setShowStartupDialog(false);
    panel.startApplication("Test", null, false, null, null, false, UNIT_TEST_USER);
    assertNotNull(panel.getEntityPanel(TestDomain.T_EMP));

    panel.logout();
  }
}
