/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.TestDomain.Detail;
import is.codion.swing.framework.ui.TestDomain.Employee;
import is.codion.swing.framework.ui.TestDomain.Master;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import java.util.Enumeration;
import java.util.List;

import static is.codion.swing.framework.ui.EntityApplicationBuilder.entityApplicationBuilder;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EntityApplicationPanelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  @AfterEach
  void tearDown() {
    Thread.setDefaultUncaughtExceptionHandler(null);
  }

  @Test
  void createDependencyTreeModel() {
    TreeModel model = EntityApplicationPanel.createDependencyTreeModel(CONNECTION_PROVIDER.entities());
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
    Enumeration<?> tree = root.preorderEnumeration();
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.nextElement();
    assertNull(node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Master.TYPE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Detail.TYPE, node.getUserObject());
  }

  @Test
  void test() throws Exception {
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set(TestDomain.class.getName());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
    EntityApplicationModel.SAVE_DEFAULT_USERNAME.set(false);
    entityApplicationBuilder(TestApplicationModel.class, TestApplicationPanel.class)
            .automaticLoginUser(UNIT_TEST_USER)
            .loginRequired(false)
            .displayFrame(false)
            .includeMainMenu(true)
            .displayStartupDialog(false)
            .start();
  }

  private static final class TestApplicationModel extends SwingEntityApplicationModel {

    public TestApplicationModel(EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
      addEntityModel(new SwingEntityModel(Employee.TYPE, connectionProvider));
    }
  }

  private static final class TestApplicationPanel extends EntityApplicationPanel<TestApplicationModel> {

    public TestApplicationPanel(TestApplicationModel applicationModel) {
      super(applicationModel);
    }

    @Override
    protected List<EntityPanel> createEntityPanels(TestApplicationModel applicationModel) {
      return singletonList(new EntityPanel(applicationModel.entityModel(Employee.TYPE)));
    }
  }
}
