/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
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

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EntityApplicationPanelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
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
  void test() {
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
    EntityApplicationPanel.builder(TestApplicationModel.class, TestApplicationPanel.class)
            .automaticLoginUser(UNIT_TEST_USER)
            .domainType(TestDomain.DOMAIN)
            .setUncaughtExceptionHandler(false)
            .saveDefaultUsername(false)
            .loginRequired(false)
            .displayFrame(false)
            .includeMainMenu(true)
            .displayStartupDialog(false)
            .start(false);
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
    protected List<EntityPanel> createEntityPanels() {
      return singletonList(new EntityPanel(applicationModel().entityModel(Employee.TYPE)));
    }
  }
}
