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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
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
