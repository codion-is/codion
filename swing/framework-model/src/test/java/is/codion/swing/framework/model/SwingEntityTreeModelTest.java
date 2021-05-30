/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.model.tests.TestDomain;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SwingEntityTreeModelTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void test() {
    final EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(
            DatabaseFactory.getDatabase()).setUser(UNIT_TEST_USER).setDomainClassName(TestDomain.class.getName());

    final SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_EMP, connectionProvider);
    final SwingEntityTreeModel treeModel = new SwingEntityTreeModel(tableModel, TestDomain.EMP_MGR_FK);

    tableModel.refresh();

    final SwingEntityTreeModel.EntityTreeNode root = (SwingEntityTreeModel.EntityTreeNode) treeModel.getRoot().children().nextElement();
    assertEquals("KING", root.getEntity().get(TestDomain.EMP_NAME));

    tableModel.setSelectedByKey(Collections.singletonList(connectionProvider.getEntities().primaryKey(TestDomain.T_EMP, 3)));//Jones

    final SwingEntityTreeModel.EntityTreeNode node = (SwingEntityTreeModel.EntityTreeNode) treeModel.getTreeSelectionModel().getSelectionPath().getLastPathComponent();
    assertEquals("JONES", node.getEntity().get(TestDomain.EMP_NAME));
  }
}
