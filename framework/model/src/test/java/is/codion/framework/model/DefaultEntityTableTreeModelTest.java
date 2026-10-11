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
package is.codion.framework.model;

import is.codion.common.model.component.tree.NodePath;
import is.codion.common.utilities.Operator;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.DefaultEntityTableModelTest.TestEntityEditModel;
import is.codion.framework.model.DefaultEntityTableModelTest.TestEntityTableModel;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityTableTreeModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void parent() {
		TestEntityTableModel tableModel = tableModel();
		// not referencing the table model entity type from itself
		assertThrows(IllegalArgumentException.class, () -> EntityTableTreeModel.builder()
						.tableModel(tableModel)
						.parent(Employee.DEPARTMENT_FK));
		EntityTableTreeModel tree = tree(tableModel);
		assertFalse(tree.orphansAsRoots().is());
		assertFalse(tree.parentsFirst().is());
	}

	@Test
	void structure() {
		TestEntityTableModel tableModel = tableModel();
		EntityTableTreeModel tree = tree(tableModel);
		assertTrue(tree.nodes().children(nodePath()).isEmpty());
		tableModel.items().refresh();
		assertEquals(names("KING"), items(tree, nodePath()));
		NodePath<Entity> king = nodePath(singletonList(employee(tableModel, "KING")));
		// the leaves from the items, before loading
		assertFalse(tree.nodes().leaf(king));
		// siblings in the order of the rows, by department then name
		assertEquals(names("CLARK", "JONES", "WARD"), items(tree, king));
		NodePath<Entity> jones = king.child(employee(tableModel, "JONES"));
		assertTrue(tree.nodes().leaf(king.child(employee(tableModel, "WARD"))));
		assertEquals(names("BLAKE", "ADAMS", "FORD", "PAUL", "SCOTT", "MARTIN"), items(tree, jones));
		assertEquals(names("BAKER", "JAMES", "TREVOR", "ALLEN", "TURNER"),
						items(tree, jones.child(employee(tableModel, "BLAKE"))));
		// the nodes hold the rows
		assertSame(employee(tableModel, "KING"), tree.nodes().children(nodePath()).get(0).item());
	}

	@Test
	void order() {
		TestEntityTableModel tableModel = tableModel();
		tableModel.items().refresh();
		EntityTableTreeModel tree = tree(tableModel);
		NodePath<Entity> king = nodePath(singletonList(employee(tableModel, "KING")));
		// sorting the table sorts the tree
		tableModel.sort().descending(Employee.NAME);
		assertEquals(names("WARD", "JONES", "CLARK"), items(tree, king));
	}

	@Test
	void parentsFirst() {
		TestEntityTableModel tableModel = tableModel();
		tableModel.items().refresh();
		tableModel.sort().descending(Employee.NAME);
		EntityTableTreeModel tree = EntityTableTreeModel.builder()
						.tableModel(tableModel)
						.parent(Employee.MGR_FK)
						.parentsFirst(true)
						.build();
		assertTrue(tree.parentsFirst().is());
		// those with children first, in the order of the rows within each group
		assertEquals(names("JONES", "CLARK", "WARD"), items(tree, path(tableModel, "KING")));
		tree.parentsFirst().set(false);
		assertEquals(names("WARD", "JONES", "CLARK"), items(tree, path(tableModel, "KING")));
		tree.parentsFirst().set(true);
		assertEquals(names("JONES", "CLARK", "WARD"), items(tree, path(tableModel, "KING")));
		// the roots as well, JONES' reports shown as roots
		tableModel.filters().get(Employee.NAME).operator().set(Operator.NOT_EQUAL);
		tableModel.filters().get(Employee.NAME).operands().equal().set("JONES");
		tree.orphansAsRoots().set(true);
		assertEquals(names("KING", "BLAKE", "SCOTT", "PAUL", "MARTIN", "FORD", "ADAMS"), items(tree, nodePath()));
	}

	@Test
	void orphans() {
		TestEntityTableModel tableModel = tableModel();
		tableModel.items().refresh();
		EntityTableTreeModel tree = tree(tableModel);
		// an excluded parent hides everything below it
		tableModel.filters().get(Employee.NAME).operator().set(Operator.NOT_EQUAL);
		tableModel.filters().get(Employee.NAME).operands().equal().set("JONES");
		assertEquals(names("KING"), items(tree, nodePath()));
		assertEquals(names("CLARK", "WARD"), items(tree, path(tableModel, "KING")));
		// an orphan, and anything below it, is not in the tree
		tableModel.selection().item().set(employee(tableModel, "ALLEN"));
		assertFalse(tree.selection().present().is());
		// shown as roots, in the order of the rows, by department then name, the table selection selected
		tree.orphansAsRoots().set(true);
		assertEquals(names("BLAKE", "KING", "ADAMS", "FORD", "PAUL", "SCOTT", "MARTIN"), items(tree, nodePath()));
		assertEquals(path(tableModel, "BLAKE", "ALLEN"), tree.selection().path().get());
		// the expansion kept
		assertTrue(tree.expansion().expanded(path(tableModel, "KING")));
		// hidden again, the table selection kept
		tree.orphansAsRoots().set(false);
		assertEquals(names("KING"), items(tree, nodePath()));
		assertFalse(tree.selection().present().is());
		assertEquals(singletonList(employee(tableModel, "ALLEN")), tableModel.selection().items().get());
		tableModel.selection().item().set(employee(tableModel, "MILLER"));
		assertEquals(path(tableModel, "KING", "CLARK", "MILLER"), tree.selection().path().get());
	}

	@Test
	void unreachable() {
		TestEntityTableModel tableModel = tableModel();
		tableModel.items().refresh();
		EntityTableTreeModel tree = tree(tableModel);
		assertTrue(tree.unreachable().get().isEmpty());
		List<List<String>> roots = new ArrayList<>();
		tree.unreachable().addListener(() -> roots.add(items(tree, nodePath())));
		// an excluded parent makes everything below it unreachable
		tableModel.filters().get(Employee.NAME).operator().set(Operator.NOT_EQUAL);
		tableModel.filters().get(Employee.NAME).operands().equal().set("JONES");
		assertEquals(new HashSet<>(names("BLAKE", "ADAMS", "FORD", "PAUL", "SCOTT", "MARTIN", "BAKER", "JAMES", "TREVOR", "ALLEN", "TURNER")),
						names(tree.unreachable().get()));
		// none with the orphans shown as roots, notified once the tree has been rebuilt
		tree.orphansAsRoots().set(true);
		assertTrue(tree.unreachable().get().isEmpty());
		assertEquals(names("BLAKE", "KING", "ADAMS", "FORD", "PAUL", "SCOTT", "MARTIN"), roots.get(roots.size() - 1));
		// a table selection made by a listener is followed by the tree
		tree.unreachable().addListener(() -> tableModel.selection().item().set(employee(tableModel, "MILLER")));
		tree.orphansAsRoots().set(false);
		assertEquals(11, tree.unreachable().get().size());
		assertEquals(names("KING"), roots.get(roots.size() - 1));
		assertEquals(path(tableModel, "KING", "CLARK", "MILLER"), tree.selection().path().get());
		tableModel.filters().get(Employee.NAME).operands().equal().set(null);
		assertTrue(tree.unreachable().get().isEmpty());
	}

	@Test
	void cycles() {
		Entity root = employee(1, "ROOT", null);
		Entity first = employee(2, "FIRST", 3);
		Entity second = employee(3, "SECOND", 2);
		Entity below = employee(4, "BELOW", 2);
		Entity self = employee(5, "SELF", 5);
		TestEntityTableModel tableModel = tableModel();
		// hidden, even with orphans shown as roots
		EntityTableTreeModel tree = EntityTableTreeModel.builder()
						.tableModel(tableModel)
						.parent(Employee.MGR_FK)
						.orphansAsRoots(true)
						.build();
		tableModel.items().set(asList(root, first, second, below, self));
		assertEquals(names("ROOT"), items(tree, nodePath()));
		assertEquals(new HashSet<>(asList(first, second, below, self)), new HashSet<>(tree.unreachable().get()));
		for (Entity entity : asList(first, second, below, self)) {
			tableModel.selection().item().set(entity);
			assertFalse(tree.selection().present().is());
		}
		tableModel.selection().items().set(asList(root, first));
		assertEquals(singletonList(nodePath(singletonList(root))), tree.selection().paths().get());
	}

	@Test
	void duplicates() {
		Entity root = employee(1, "ROOT", null);
		Entity child = employee(2, "CHILD", 1);
		TestEntityTableModel tableModel = tableModel();
		EntityTableTreeModel tree = tree(tableModel);
		// siblings must be distinct, the first of equal rows in the tree
		tableModel.items().set(asList(root, child, root.copy().builder().build(), child.copy().builder().build()));
		assertEquals(names("ROOT"), items(tree, nodePath()));
		assertSame(root, tree.nodes().children(nodePath()).get(0).item());
		assertEquals(names("CHILD"), items(tree, nodePath(singletonList(root))));
	}

	@Test
	void selection() {
		TestEntityTableModel tableModel = tableModel();
		tableModel.items().refresh();
		// the selection made before the tree is built
		tableModel.selection().item().set(employee(tableModel, "MILLER"));
		EntityTableTreeModel tree = tree(tableModel);
		assertEquals(path(tableModel, "KING", "CLARK", "MILLER"), tree.selection().path().get());
		// the table to the tree, the ancestors expanded
		tableModel.selection().items().set(asList(employee(tableModel, "ALLEN"), employee(tableModel, "WARD")));
		assertTrue(tree.expansion().expanded(path(tableModel, "KING", "JONES", "BLAKE")));
		assertEquals(asList(path(tableModel, "KING", "JONES", "BLAKE", "ALLEN"), path(tableModel, "KING", "WARD")),
						tree.selection().paths().get());
		// the tree to the table
		tree.selection().path().set(path(tableModel, "KING", "JONES"));
		assertEquals(singletonList(employee(tableModel, "JONES")), tableModel.selection().items().get());
		tree.selection().clear();
		assertFalse(tableModel.selection().present().is());
	}

	@Test
	void treeSelectionByItem() {
		TestEntityTableModel tableModel = tableModel();
		tableModel.items().refresh();
		EntityTableTreeModel tree = tree(tableModel);
		// nothing expanded, ALLEN not in the tree model, its parent not loaded
		NodePath<Entity> allen = path(tableModel, "KING", "JONES", "BLAKE", "ALLEN");
		assertFalse(tree.nodes().contains(allen));
		tree.selection().item().set(employee(tableModel, "ALLEN"));
		assertEquals(allen, tree.selection().path().get());
		assertTrue(tree.expansion().expanded(allen.parent()));
		assertEquals(singletonList(employee(tableModel, "ALLEN")), tableModel.selection().items().get());
		// several, one in the tree model, one not, in row order
		tree.expansion().collapse(path(tableModel, "KING"));
		NodePath<Entity> miller = path(tableModel, "KING", "CLARK", "MILLER");
		assertFalse(tree.nodes().contains(miller));
		tree.selection().items().set(asList(employee(tableModel, "ALLEN"), employee(tableModel, "MILLER")));
		assertEquals(asList(miller, allen), tree.selection().paths().get());
		tree.selection().clear();
		tree.selection().items().add(employee(tableModel, "WARD"));
		assertEquals(singletonList(path(tableModel, "KING", "WARD")), tree.selection().paths().get());
		// an entity not in the tree, an orphan not shown as a root, is ignored
		tableModel.filters().get(Employee.NAME).operands().equal().set("JONES");
		tableModel.filters().get(Employee.NAME).operator().set(Operator.NOT_EQUAL);
		tableModel.filters().get(Employee.NAME).enabled().set(true);
		tree.selection().items().set(asList(employee(tableModel, "ALLEN"), employee(tableModel, "MILLER")));
		assertEquals(singletonList(miller), tree.selection().paths().get());
	}

	@Test
	void singleSelection() {
		TestEntityTableModel tableModel = tableModel();
		tableModel.items().refresh();
		tableModel.selection().singleSelection().set(true);
		// the mode of the table when built
		EntityTableTreeModel tree = tree(tableModel);
		assertTrue(tree.selection().singleSelection().is());
		tableModel.selection().singleSelection().set(false);
		assertFalse(tree.selection().singleSelection().is());
		tableModel.selection().items().set(asList(employee(tableModel, "ALLEN"), employee(tableModel, "WARD")));
		assertEquals(2, tree.selection().count());
		// changing the mode clears both selections
		tableModel.selection().singleSelection().set(true);
		assertTrue(tree.selection().singleSelection().is());
		assertFalse(tableModel.selection().present().is());
		assertFalse(tree.selection().present().is());
		// one way, the tree does not change the mode of the table
		tree.selection().singleSelection().set(false);
		assertTrue(tableModel.selection().singleSelection().is());
	}

	@Test
	void followsItems() {
		TestEntityTableModel tableModel = tableModel();
		tableModel.items().refresh();
		EntityTableTreeModel tree = EntityTableTreeModel.builder()
						.tableModel(tableModel)
						.parent(Employee.MGR_FK)
						.orphansAsRoots(true)
						.build();
		tree.expansion().expand(path(tableModel, "KING", "JONES", "BLAKE"));
		tableModel.selection().item().set(employee(tableModel, "ALLEN"));
		// moved, from BLAKE to CLARK, staying selected
		Entity allen = employee(tableModel, "ALLEN").copy().builder()
						.with(Employee.MGR, employee(tableModel, "CLARK").get(Employee.ID))
						.build();
		tableModel.replace(singletonList(allen));
		assertFalse(items(tree, path(tableModel, "KING", "JONES", "BLAKE")).contains("ALLEN"));
		assertEquals(names("MILLER", "ALLEN"), items(tree, path(tableModel, "KING", "CLARK")));
		assertEquals(path(tableModel, "KING", "CLARK", "ALLEN"), tree.selection().path().get());
		assertEquals(singletonList(allen), tableModel.selection().items().get());
		// the expansion kept
		assertTrue(tree.expansion().expanded(path(tableModel, "KING", "JONES", "BLAKE")));
		// removed
		tableModel.items().remove(employee(tableModel, "BLAKE"));
		assertFalse(items(tree, path(tableModel, "KING", "JONES")).contains("BLAKE"));
		// BLAKE's reports now orphans
		assertEquals(names("BAKER", "JAMES", "KING", "TREVOR", "TURNER"), items(tree, nodePath()));
		// added
		Entity added = employee(100, "ADDED", employee(tableModel, "WARD").get(Employee.ID));
		tableModel.items().add(added);
		assertEquals(names("ADDED"), items(tree, path(tableModel, "KING", "WARD")));
		// refreshed, the selection kept, BLAKE back
		tableModel.items().refresh();
		assertEquals(names("KING"), items(tree, nodePath()));
		assertEquals(path(tableModel, "KING", "JONES", "BLAKE", "ALLEN"), tree.selection().path().get());
		assertTrue(tree.nodes().children(path(tableModel, "KING", "WARD")).isEmpty());
		// cleared
		tableModel.items().clear();
		assertEquals(emptyList(), items(tree, nodePath()));
	}

	@Test
	void notRetainedByTableModel() {
		TestEntityTableModel tableModel = tableModel();
		tableModel.items().refresh();
		WeakReference<EntityTableTreeModel> reference = new WeakReference<>(tree(tableModel));
		for (int i = 0; i < 20 && reference.get() != null; i++) {
			System.gc();
		}
		assertNull(reference.get());
		// the table model unaffected
		tableModel.selection().item().set(employee(tableModel, "KING"));
		tableModel.items().refresh();
		assertEquals(singletonList(employee(tableModel, "KING")), tableModel.selection().items().get());
	}

	private static TestEntityTableModel tableModel() {
		return new TestEntityTableModel(new TestEntityEditModel(Employee.TYPE, CONNECTION));
	}

	private static EntityTableTreeModel tree(TestEntityTableModel tableModel) {
		return EntityTableTreeModel.builder()
						.tableModel(tableModel)
						.parent(Employee.MGR_FK)
						.build();
	}

	private static Entity employee(TestEntityTableModel tableModel, String name) {
		return tableModel.items().get().stream()
						.filter(employee -> employee.get(Employee.NAME).equals(name))
						.findFirst()
						.orElseThrow(IllegalArgumentException::new);
	}

	private static Entity employee(int id, String name, Integer manager) {
		return CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, id)
						.with(Employee.NAME, name)
						.with(Employee.MGR, manager)
						.build();
	}

	private static NodePath<Entity> path(TestEntityTableModel tableModel, String... names) {
		return nodePath(asList(names).stream()
						.map(name -> employee(tableModel, name))
						.collect(toList()));
	}

	private static List<String> items(EntityTableTreeModel tree, NodePath<Entity> parent) {
		if (!parent.root()) {
			tree.expansion().expand(parent);
		}

		return tree.nodes().children(parent).stream()
						.map(path -> path.item().get(Employee.NAME))
						.collect(toList());
	}

	private static List<String> names(String... names) {
		return Collections.unmodifiableList(asList(names));
	}

	private static Set<String> names(Collection<Entity> entities) {
		return entities.stream()
						.map(entity -> entity.get(Employee.NAME))
						.collect(toSet());
	}
}
