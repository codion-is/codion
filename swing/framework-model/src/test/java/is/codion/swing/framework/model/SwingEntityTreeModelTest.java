/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Employee;

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

    SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, connectionProvider);
    SwingEntityTreeModel treeModel = new SwingEntityTreeModel(tableModel, Employee.MGR_FK);

    tableModel.refresh();

    SwingEntityTreeModel.EntityTreeNode root = (SwingEntityTreeModel.EntityTreeNode) treeModel.getRoot().children().nextElement();
    assertEquals("KING", root.entity().get(Employee.NAME));

    tableModel.selectByKey(Collections.singletonList(connectionProvider.entities().primaryKey(Employee.TYPE, 3)));//Jones

    SwingEntityTreeModel.EntityTreeNode node = (SwingEntityTreeModel.EntityTreeNode) treeModel.treeSelectionModel().getSelectionPath().getLastPathComponent();
    assertEquals("JONES", node.entity().get(Employee.NAME));
  }
}
