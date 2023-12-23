/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class SwingEntityTreeModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void test() throws DatabaseException {
    EntityConnectionProvider connectionProvider = LocalEntityConnectionProvider.builder()
            .domain(new TestDomain())
            .user(UNIT_TEST_USER)
            .build();

    SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, connectionProvider);

    assertThrows(IllegalArgumentException.class, () -> SwingEntityTreeModel.swingEntityTreeModel(tableModel, Detail.MASTER_FK));

    SwingEntityTreeModel treeModel = SwingEntityTreeModel.swingEntityTreeModel(tableModel, Employee.MGR_FK);

    tableModel.refresh();

    SwingEntityTreeModel.EntityTreeNode root = (SwingEntityTreeModel.EntityTreeNode) treeModel.getRoot().children().nextElement();
    assertEquals("KING", root.entity().get(Employee.NAME));

    tableModel.select(Collections.singletonList(connectionProvider.entities().primaryKey(Employee.TYPE, 3)));//Jones

    SwingEntityTreeModel.EntityTreeNode node = (SwingEntityTreeModel.EntityTreeNode) treeModel.treeSelectionModel().getSelectionPath().getLastPathComponent();
    assertEquals("JONES", node.entity().get(Employee.NAME));

    List<Entity> king = connectionProvider.connection().select(Employee.ID.equalTo(8));
    treeModel.refreshSelect(king);//King
    assertEquals(king, tableModel.selectionModel().getSelectedItems());
  }
}
