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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.component.tree.NodePath;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.swing.framework.model.component.SwingEntityTableTreeModel;

import org.junit.jupiter.api.Test;

import javax.swing.ListSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Swing {@code TreeModel} coat of {@link SwingEntityTableTreeModel} over a {@link SwingEntityTableModel}, the
 * logic is tested in {@code is.codion.framework.model.DefaultEntityTableTreeModelTest}.
 */
public final class DefaultSwingEntityTableTreeModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void treeModelCoat() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		tableModel.items().refresh();
		SwingEntityTableTreeModel tree = SwingEntityTableTreeModel.builder()
						.tableModel(tableModel)
						.parent(Employee.MGR_FK)
						.build();
		// the top of the hierarchy, via the TreeModel
		assertEquals(1, tree.getChildCount(tree.getRoot()));
		NodePath<Entity> king = (NodePath<Entity>) tree.getChild(tree.getRoot(), 0);
		assertEquals("KING", king.item().get(Employee.NAME));
		assertFalse(tree.isLeaf(king));
		// the table to the tree, via the TreeSelectionModel
		tableModel.selection().item().set(king.item());
		assertEquals(new TreePath(new Object[] {tree.getRoot(), king}), tree.selection().getSelectionPath());
		// the tree to the table
		tree.expansion().expand(king);
		NodePath<Entity> jones = (NodePath<Entity>) tree.getChild(king, 1);
		assertEquals("JONES", jones.item().get(Employee.NAME));
		tree.selection().setSelectionPath(tree.treePath(jones));
		assertEquals(jones.item(), tableModel.selection().item().get());
	}

	@Test
	void singleSelection() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		tableModel.items().refresh();
		SwingEntityTableTreeModel tree = SwingEntityTableTreeModel.builder()
						.tableModel(tableModel)
						.parent(Employee.MGR_FK)
						.build();
		tableModel.selection().items().set(asList(tableModel.items().included().get(0), tableModel.items().included().get(1)));
		assertEquals(2, tree.selection().getSelectionCount());
		// the list selection model mode followed by the tree selection model, both selections cleared
		tableModel.selection().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		assertEquals(TreeSelectionModel.SINGLE_TREE_SELECTION, tree.selection().getSelectionMode());
		assertTrue(tableModel.selection().isSelectionEmpty());
		assertTrue(tree.selection().isSelectionEmpty());
		tableModel.selection().singleSelection().set(false);
		assertEquals(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION, tree.selection().getSelectionMode());
	}
}
