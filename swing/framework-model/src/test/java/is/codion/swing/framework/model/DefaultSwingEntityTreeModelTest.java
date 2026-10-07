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
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.swing.framework.model.component.SwingEntityTreeModel;

import org.junit.jupiter.api.Test;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Swing {@code TreeModel} coat of {@link SwingEntityTreeModel}, the entity logic is tested in
 * {@code is.codion.framework.model.DefaultEntityTreeModelTest}.
 */
public final class DefaultSwingEntityTreeModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void treeModelCoat() {
		SwingEntityTreeModel model = SwingEntityTreeModel.builder()
						.roots(Department.TYPE)
						.connection(CONNECTION)
						.children(Employee.DEPARTMENT_FK)
						.children(Employee.MGR_FK)
						.build();
		assertSame(CONNECTION, model.connection());
		List<String> events = new ArrayList<>();
		model.addTreeModelListener(new TreeModelListener() {
			@Override
			public void treeNodesChanged(TreeModelEvent event) {
				events.add("changed");
			}

			@Override
			public void treeNodesInserted(TreeModelEvent event) {
				events.add("inserted");
			}

			@Override
			public void treeNodesRemoved(TreeModelEvent event) {
				events.add("removed");
			}

			@Override
			public void treeStructureChanged(TreeModelEvent event) {
				events.add("structureChanged");
			}
		});
		model.nodes().refresh();
		assertEquals(singletonList("inserted"), events);
		NodePath<Entity> root = model.getRoot();
		assertSame(nodePath(), root);
		int departments = CONNECTION.count(EntityConnection.Count.all(Department.TYPE));
		assertEquals(departments, model.getChildCount(root));
		NodePath<Entity> first = (NodePath<Entity>) model.getChild(root, 0);
		assertEquals(0, model.getIndexOfChild(root, first));
		assertEquals(Department.TYPE, first.item().type());
		TreePath treePath = model.treePath(first);
		assertEquals(2, treePath.getPathCount());
		assertSame(first, treePath.getLastPathComponent());

		//the selection a TreeSelectionModel, by item
		model.selection().item().set(first.item());
		assertTrue(model.selection().isPathSelected(treePath));
		assertEquals(first, model.selection().path().get());

		//a department with employees, expanded, inserting them
		NodePath<Entity> withEmployees = model.visible().get().stream()
						.filter(path -> !model.isLeaf(path))
						.findFirst()
						.orElseThrow(IllegalStateException::new);
		events.clear();
		model.expansion().expand(withEmployees);
		assertEquals(singletonList("inserted"), events);
		assertTrue(model.getChildCount(withEmployees) > 0);
		NodePath<Entity> employee = (NodePath<Entity>) model.getChild(withEmployees, 0);
		assertEquals(Employee.TYPE, employee.item().type());
		model.fireNodesChanged(singletonList(employee));
		assertEquals("changed", events.get(events.size() - 1));
	}
}
