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
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.model.DetailDomain.Department;
import is.codion.framework.model.DetailDomain.DepartmentExtra;
import is.codion.framework.model.DetailDomain.Employee;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityTreeModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new DetailDomain())
					.user(UNIT_TEST_USER)
					.build();

	@AfterEach
	void rollback() {
		if (CONNECTION.transactionOpen()) {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void selfReferencing() {
		EntityTreeModel model = EntityTreeModel.builder()
						.roots(Employee.TYPE)
						.connection(CONNECTION)
						.children(Employee.MANAGER_FK)
						.build();
		assertSame(CONNECTION, model.connection());
		model.nodes().refresh();
		//the top of the hierarchy, nobody to report to
		assertEquals(singletonList(path("KING")), model.visible().get());
		expandAll(model);

		List<Entity> employees = CONNECTION.select(Select.all(Employee.TYPE).build());
		Set<Integer> managers = employees.stream()
						.map(employee -> employee.get(Employee.MANAGER_ID))
						.filter(Objects::nonNull)
						.collect(toSet());
		//every employee, a leaf when nobody reports to them, which were never loaded
		assertEquals(employees.size(), model.visible().size());
		for (NodePath<Entity> path : model.visible().get()) {
			boolean manager = managers.contains(path.item().get(Employee.ID));
			assertEquals(!manager, model.nodes().leaf(path), path.toString());
			assertEquals(manager, model.nodes().loaded(path), path.toString());
		}
		assertEquals(asList(path("KING", "JONES"), path("KING", "WARD"), path("KING", "CLARK")).stream()
						.collect(toSet()), model.nodes().children(path("KING")).stream().collect(toSet()));
	}

	@Test
	void masterDetail() {
		EntityTreeModel model = EntityTreeModel.builder()
						.roots(Department.TYPE, roots -> roots
										.select(select -> select.orderBy(OrderBy.descending(Department.ID))))
						.connection(CONNECTION)
						.children(Employee.DEPARTMENT_FK)
						.build();
		model.nodes().refresh();
		List<NodePath<Entity>> departments = model.visible().get();
		//the query order kept
		assertEquals(asList(40, 30, 20, 10), departments.stream()
						.map(path -> path.item().get(Department.ID))
						.collect(toList()));
		//no employees in operations, found without loading it
		NodePath<Entity> operations = departments.get(0);
		assertTrue(model.nodes().leaf(operations));
		assertFalse(model.nodes().loaded(operations));
		NodePath<Entity> accounting = departments.get(3);
		assertFalse(model.nodes().leaf(accounting));
		model.expansion().expand(accounting);
		List<NodePath<Entity>> employees = model.nodes().children(accounting);
		assertEquals(7, employees.size());
		//no foreign key references employees, leaves without a query
		employees.forEach(employee -> {
			assertTrue(model.nodes().leaf(employee));
			assertFalse(model.nodes().loaded(employee));
		});
	}

	@Test
	void mixed() {
		//the employees by department, and by manager, so they repeat
		EntityTreeModel model = EntityTreeModel.builder()
						.roots(Department.TYPE)
						.connection(CONNECTION)
						.children(Employee.DEPARTMENT_FK)
						.children(Employee.MANAGER_FK)
						.build();
		model.nodes().refresh();
		expandAll(model);
		Entity jones = employee("JONES");
		//below research, and below king, below accounting
		assertEquals(new HashSet<>(asList(asList(department(10), employee("KING"), jones), asList(department(20), jones))),
						model.nodes().paths(jones).stream()
										.map(NodePath::items)
										.collect(toSet()));
		//selecting by item, the first visible occurrence
		model.selection().item().set(jones);
		assertEquals(model.visible().get().stream()
						.filter(path -> path.item().equals(jones))
						.findFirst()
						.orElseThrow(IllegalStateException::new), model.selection().path().get());
	}

	@Test
	void siblingTypes() {
		CONNECTION.startTransaction();
		Entity research = department(20);
		CONNECTION.insert(CONNECTION.entities().entity(DepartmentExtra.TYPE)
						.with(DepartmentExtra.DEPARTMENT_FK, research)
						.with(DepartmentExtra.DESCRIPTION, "extra")
						.build());
		//the extras configured first, coming before the employees
		EntityTreeModel model = EntityTreeModel.builder()
						.roots(Department.TYPE)
						.connection(CONNECTION)
						.children(DepartmentExtra.DEPARTMENT_FK)
						.children(Employee.DEPARTMENT_FK)
						.build();
		model.nodes().refresh();
		NodePath<Entity> path = nodePath(singletonList(research));
		model.expansion().expand(path);
		List<NodePath<Entity>> children = model.nodes().children(path);
		assertEquals(6, children.size());
		assertEquals(DepartmentExtra.TYPE, children.get(0).item().type());
		children.subList(1, children.size()).forEach(child -> assertEquals(Employee.TYPE, child.item().type()));
		//no foreign key references either type, all leaves
		children.forEach(child -> assertTrue(model.nodes().leaf(child)));
	}

	@Test
	void ordering() {
		Comparator<Entity> byName = comparing(employee -> employee.get(Employee.NAME));
		//the roots by their order by, the children, of the same type, by their comparator
		EntityTreeModel model = EntityTreeModel.builder()
						.roots(Employee.TYPE, roots -> roots
										.condition(() -> Employee.NAME.in("JONES", "CLARK", "WARD"))
										.select(select -> select.orderBy(OrderBy.descending(Employee.NAME))))
						.connection(CONNECTION)
						.children(Employee.MANAGER_FK, children -> children.comparator(byName))
						.build();
		model.nodes().refresh();
		assertEquals(asList("WARD", "JONES", "CLARK"), names(model.nodes().children(nodePath())));
		NodePath<Entity> jones = model.nodes().children(nodePath()).get(1);
		model.expansion().expand(jones);
		assertEquals(asList("ADAMS", "BLAKE", "FORD", "MARTIN", "PAUL", "SCOTT"), names(model.nodes().children(jones)));

		//the comparator of the tree for the children, specifying neither a comparator nor an order by,
		//the order by of the roots winning
		model = EntityTreeModel.builder()
						.roots(Employee.TYPE, roots -> roots
										.condition(() -> Employee.NAME.in("JONES", "CLARK", "WARD"))
										.select(select -> select.orderBy(OrderBy.ascending(Employee.NAME))))
						.connection(CONNECTION)
						.children(Employee.MANAGER_FK)
						.comparator(byName.reversed())
						.build();
		model.nodes().refresh();
		assertEquals(asList("CLARK", "JONES", "WARD"), names(model.nodes().children(nodePath())));
		jones = model.nodes().children(nodePath()).get(1);
		model.expansion().expand(jones);
		assertEquals(asList("SCOTT", "PAUL", "MARTIN", "FORD", "BLAKE", "ADAMS"), names(model.nodes().children(jones)));

		//the comparator of the foreign key winning over its order by
		model = EntityTreeModel.builder()
						.roots(Employee.TYPE)
						.connection(CONNECTION)
						.children(Employee.MANAGER_FK, children -> children
										.select(select -> select.orderBy(OrderBy.descending(Employee.NAME)))
										.comparator(byName))
						.build();
		model.nodes().refresh();
		model.expansion().expand(path("KING"));
		assertEquals(asList("CLARK", "JONES", "WARD"), names(model.nodes().children(path("KING"))));

		//replacing the ordering entirely, the last one set winning
		model = EntityTreeModel.builder()
						.roots(Employee.TYPE)
						.connection(CONNECTION)
						.children(Employee.MANAGER_FK, children -> children.comparator(byName))
						.comparators(parent -> byName.reversed())
						.build();
		model.nodes().refresh();
		model.expansion().expand(path("KING"));
		assertEquals(asList("WARD", "JONES", "CLARK"), names(model.nodes().children(path("KING"))));
		model = EntityTreeModel.builder()
						.roots(Employee.TYPE)
						.connection(CONNECTION)
						.children(Employee.MANAGER_FK)
						.comparators(parent -> byName.reversed())
						.comparator(byName)
						.build();
		model.nodes().refresh();
		model.expansion().expand(path("KING"));
		assertEquals(asList("CLARK", "JONES", "WARD"), names(model.nodes().children(path("KING"))));
	}

	@Test
	void conditions() {
		EntityTreeModel model = EntityTreeModel.builder()
						.roots(Employee.TYPE, roots -> roots
										.condition(() -> Employee.NAME.equalTo("KING")))
						.connection(CONNECTION)
						.children(Employee.MANAGER_FK, children -> children
										.condition(() -> Employee.SALARY.greaterThan(1500d))
										.select(select -> select.orderBy(OrderBy.descending(Employee.NAME))))
						.build();
		model.nodes().refresh();
		model.expansion().expand(path("KING"));
		//the query order kept, all earning more than 1500
		assertEquals(asList(path("KING", "JONES"), path("KING", "CLARK")), model.nodes().children(path("KING")));
		//clark's only report, miller, earns less, so clark is a leaf, by the leaf query
		assertTrue(model.nodes().leaf(path("KING", "CLARK")));
		assertFalse(model.nodes().leaf(path("KING", "JONES")));

		//a roots condition replacing the default, the top of the hierarchy
		EntityTreeModel blake = EntityTreeModel.builder()
						.roots(Employee.TYPE, roots -> roots
										.condition(() -> Employee.NAME.equalTo("BLAKE")))
						.connection(CONNECTION)
						.children(Employee.MANAGER_FK)
						.build();
		blake.nodes().refresh();
		assertEquals(singletonList(path("BLAKE")), blake.visible().get());
		blake.expansion().expand(path("BLAKE"));
		assertEquals(5, blake.nodes().children(path("BLAKE")).size());
	}

	@Test
	void cycle() {
		CONNECTION.startTransaction();
		//king reporting to jones, who reports to king
		Entity king = employee("KING");
		king.set(Employee.MANAGER_FK, employee("JONES"));
		CONNECTION.update(king);
		EntityTreeModel model = EntityTreeModel.builder()
						.roots(Employee.TYPE, roots -> roots
										.condition(() -> Employee.NAME.equalTo("KING")))
						.connection(CONNECTION)
						.children(Employee.MANAGER_FK)
						.build();
		model.nodes().refresh();
		model.expansion().expand(path("KING"));
		model.expansion().expand(path("KING", "JONES"));
		//not below himself
		assertFalse(model.nodes().contains(path("KING", "JONES", "KING")));
		assertTrue(model.nodes().contains(path("KING", "JONES", "BLAKE")));
	}

	@Test
	void build() {
		//duplicate
		assertThrows(IllegalArgumentException.class, () -> EntityTreeModel.builder()
						.roots(Employee.TYPE)
						.connection(CONNECTION)
						.children(Employee.MANAGER_FK)
						.children(Employee.MANAGER_FK));
		//referencing neither the root type nor the type of another foreign key
		assertThrows(IllegalArgumentException.class, () -> EntityTreeModel.builder()
						.roots(Department.TYPE)
						.connection(CONNECTION)
						.children(Employee.MANAGER_FK)
						.build());
		//a condition for another entity type, when the query runs
		EntityTreeModel roots = EntityTreeModel.builder()
						.roots(Department.TYPE, departments -> departments
										.condition(() -> Employee.NAME.equalTo("KING")))
						.connection(CONNECTION)
						.build();
		assertThrows(IllegalArgumentException.class, () -> roots.nodes().refresh());
		EntityTreeModel children = EntityTreeModel.builder()
						.roots(Department.TYPE)
						.connection(CONNECTION)
						.children(Employee.DEPARTMENT_FK, employees -> employees
										.condition(() -> Department.NAME.equalTo("SALES")))
						.build();
		//the children condition applying to the leaf query, when the roots are loaded
		assertThrows(IllegalArgumentException.class, () -> children.nodes().refresh());
		//in any order, the referenced type configured later
		EntityTreeModel.builder()
						.roots(Department.TYPE)
						.connection(CONNECTION)
						.children(Employee.MANAGER_FK)
						.children(Employee.DEPARTMENT_FK)
						.build();
	}

	private static Entity employee(String name) {
		return CONNECTION.selectSingle(Employee.NAME.equalTo(name));
	}

	private static Entity department(int id) {
		return CONNECTION.selectSingle(Department.ID.equalTo(id));
	}

	private static NodePath<Entity> path(String... names) {
		NodePath<Entity> path = nodePath();
		for (String name : names) {
			path = path.child(employee(name));
		}

		return path;
	}

	private static List<String> names(List<NodePath<Entity>> paths) {
		return paths.stream()
						.map(path -> path.item().get(Employee.NAME))
						.collect(toList());
	}

	private static void expandAll(EntityTreeModel model) {
		boolean expanded;
		do {
			expanded = false;
			for (NodePath<Entity> path : model.visible().get()) {
				if (!model.nodes().leaf(path) && !model.expansion().expanded(path)) {
					model.expansion().expand(path);
					expanded = true;
				}
			}
		}
		while (expanded);
	}
}
