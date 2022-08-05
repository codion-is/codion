/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.model.test.TestDomain;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SwingEntityTreeModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void test() {
    EntityConnectionProvider connectionProvider = LocalEntityConnectionProvider.builder()
            .domainClassName(TestDomain.class.getName())
            .user(UNIT_TEST_USER)
            .build();

    SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_EMP, connectionProvider);
    SwingEntityTreeModel treeModel = new SwingEntityTreeModel(tableModel, TestDomain.EMP_MGR_FK);

    tableModel.refresh();

    SwingEntityTreeModel.EntityTreeNode root = (SwingEntityTreeModel.EntityTreeNode) treeModel.getRoot().children().nextElement();
    assertEquals("KING", root.getEntity().get(TestDomain.EMP_NAME));

    tableModel.setSelectedByKey(Collections.singletonList(connectionProvider.entities().primaryKey(TestDomain.T_EMP, 3)));//Jones

    SwingEntityTreeModel.EntityTreeNode node = (SwingEntityTreeModel.EntityTreeNode) treeModel.treeSelectionModel().getSelectionPath().getLastPathComponent();
    assertEquals("JONES", node.getEntity().get(TestDomain.EMP_NAME));
  }
}
