/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.model.EntityApplicationModel;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.model.SwingEntityModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EntityApplicationPanelTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(new TestDomain(),
          Databases.getInstance()).setUser(UNIT_TEST_USER);

  @AfterEach
  public void tearDown() {
    Thread.setDefaultUncaughtExceptionHandler(null);
  }

  @Test
  public void getDependencyTreeModel() {
    final EntityApplicationPanel panel = new EntityApplicationPanel() {
      @Override
      protected SwingEntityApplicationModel initializeApplicationModel(
              final EntityConnectionProvider connectionProvider) {
        return new SwingEntityApplicationModel(connectionProvider) {};
      }
    };
    panel.initialize(panel.initializeApplicationModel(CONNECTION_PROVIDER));
    final TreeModel model = panel.getDependencyTreeModel();
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
    final Enumeration tree = root.preorderEnumeration();
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.nextElement();
    assertNull(node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(TestDomain.T_MASTER, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(TestDomain.T_DETAIL, node.getUserObject());
  }

  @Test
  public void test() throws Exception {
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set(TestDomain.class.getName());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
    EntityApplicationModel.SAVE_DEFAULT_USERNAME.set(false);
    final EntityApplicationPanel<SwingEntityApplicationModel> panel = new EntityApplicationPanel<SwingEntityApplicationModel>() {
      @Override
      protected List<EntityPanel> initializeEntityPanels(final SwingEntityApplicationModel applicationModel) {
        return Collections.singletonList(new EntityPanel(applicationModel.getEntityModel(TestDomain.T_EMP)));
      }
      @Override
      protected SwingEntityApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
        final SwingEntityApplicationModel model = new SwingEntityApplicationModel(connectionProvider);
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
