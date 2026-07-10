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
package is.codion.swing.framework.ui;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;

import org.junit.jupiter.api.Test;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EntityViewerTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@Test
	void test() {
		try (EntityConnectionProvider connectionProvider = LocalEntityConnectionProvider.builder()
						.domain(new TestDomain())
						.user(UNIT_TEST_USER)
						.build()) {
			Entity blake = connectionProvider.connection().selectSingle(TestDomain.Employee.NAME.equalTo("BLAKE"));
			blake.set(TestDomain.Employee.SALARY, 1000d);

			JTree tree = EntityViewer.createTree(blake, connectionProvider);
			expandAll(tree, new TreePath(tree.getModel().getRoot()));
			expandAll(tree, new TreePath(tree.getModel().getRoot()));
		}
	}

	@Test
	void deletedReference() {
		try (EntityConnectionProvider connectionProvider = LocalEntityConnectionProvider.builder()
						.domain(new TestDomain())
						.user(UNIT_TEST_USER)
						.build()) {
			Entity blake = connectionProvider.connection().selectSingle(TestDomain.Employee.NAME.equalTo("BLAKE"));
			//a reference to a row which no longer exists, exactly what one opens this tool to find
			blake.set(TestDomain.Employee.DEPARTMENT_FK, connectionProvider.entities()
							.entity(TestDomain.Department.TYPE)
							.with(TestDomain.Department.ID, -42)
							.build());

			JTree tree = EntityViewer.createTree(blake, connectionProvider);
			expandAll(tree, new TreePath(tree.getModel().getRoot()));

			assertTrue(children(tree).stream()
							.filter(DefaultMutableTreeNode.class::isInstance)
							.map(DefaultMutableTreeNode.class::cast)
							.filter(node -> node.getChildCount() == 1)
							.map(node -> node.getChildAt(0).toString())
							.anyMatch("<deleted>"::equals));
		}
	}

	private static List<TreeNode> children(JTree tree) {
		List<TreeNode> children = new ArrayList<>();
		Enumeration<? extends TreeNode> nodes = ((TreeNode) tree.getModel().getRoot()).children();
		while (nodes.hasMoreElements()) {
			children.add(nodes.nextElement());
		}

		return children;
	}

	private static void expandAll(JTree tree, TreePath parent) {
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0) {
			Enumeration<? extends TreeNode> e = node.children();
			while (e.hasMoreElements()) {
				expandAll(tree, parent.pathByAddingChild(e.nextElement()));
			}
		}
		tree.expandPath(parent);
	}
}
