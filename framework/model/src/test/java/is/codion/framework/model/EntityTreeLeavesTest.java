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

import is.codion.common.model.component.tree.FilterTreeModel;
import is.codion.common.model.component.tree.NodePath;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.DetailDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The batched leaf detection planned for an entity tree, see {@code FilterTreeModel.Builder.leaves()}: a
 * self-referencing hierarchy, the employees by their manager, where one query per load finds which of the children
 * loaded have children of their own, rather than one query per node.
 */
public final class EntityTreeLeavesTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new DetailDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void selfReferencing() {
		AtomicInteger childQueries = new AtomicInteger();
		AtomicInteger leafQueries = new AtomicInteger();
		FilterTreeModel<Entity> model = FilterTreeModel.builder()
						.roots(() -> CONNECTION.select(Employee.MANAGER_FK.isNull()))
						.children(path -> {
							childQueries.incrementAndGet();
							return CONNECTION.select(Employee.MANAGER_FK.equalTo(path.item()));
						})
						.leaves(paths -> {
							leafQueries.incrementAndGet();
							//the distinct managers among the given employees, in a single query
							List<Integer> managers = CONNECTION.select(Employee.MANAGER_ID,
											Employee.MANAGER_FK.in(paths.stream()
															.map(NodePath::item)
															.collect(toList())));

							return paths.stream()
											.filter(path -> !managers.contains(path.item().get(Employee.ID)))
											.collect(toList());
						})
						.build();
		model.nodes().refresh();
		expandAll(model);

		List<Entity> employees = CONNECTION.select(Select.all(Employee.TYPE).build());
		Set<Integer> managers = employees.stream()
						.map(employee -> employee.get(Employee.MANAGER_ID))
						.filter(Objects::nonNull)
						.collect(toSet());
		//every employee in the tree, a leaf when nobody reports to them
		assertEquals(employees.size(), model.visible().size());
		for (NodePath<Entity> path : model.visible().get()) {
			assertEquals(!managers.contains(path.item().get(Employee.ID)), model.nodes().leaf(path), path.toString());
		}
		//only the managers loaded, the leaves never were
		assertEquals(managers.size(), childQueries.get());
		//one leaf query per load, the roots and each manager, rather than one per employee
		assertEquals(1 + managers.size(), leafQueries.get());
	}

	private static void expandAll(FilterTreeModel<Entity> model) {
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
