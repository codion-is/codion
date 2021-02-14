/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel.DisplayFrame;
import is.codion.swing.framework.ui.EntityApplicationPanel.MaximizeFrame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import java.util.Enumeration;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityApplicationPanelTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @AfterEach
  public void tearDown() {
    Thread.setDefaultUncaughtExceptionHandler(null);
  }

  @Test
  public void getDependencyTreeModel() {
    final EntityApplicationPanel panel = new EntityApplicationPanel() {
      @Override
      protected List<EntityPanel> initializeEntityPanels(final SwingEntityApplicationModel applicationModel) {
        return emptyList();
      }

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
        return singletonList(new EntityPanel(applicationModel.getEntityModel(TestDomain.T_EMP)));
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
    panel.startApplication("Test", null, MaximizeFrame.NO, null, null, DisplayFrame.NO, UNIT_TEST_USER);
    assertNotNull(panel.getEntityPanel(TestDomain.T_EMP));

    panel.logout();
  }
}
