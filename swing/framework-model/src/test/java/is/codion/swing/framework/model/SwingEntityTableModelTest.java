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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.filter.SortOrder;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.model.test.AbstractEntityTableModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityTableModelTest extends AbstractEntityTableModelTest<
				SwingEntityEditModel, SwingEntityTableModel, SwingEntityEditor> {

	@Override
	protected SwingEntityTableModel createTestTableModel() {
		return new SwingEntityTableModel(Detail.TYPE, testEntities, connectionProvider());
	}

	@Override
	protected SwingEntityTableModel createDepartmentTableModel() {
		SwingEntityTableModel deptModel = createTableModel(Department.TYPE, testModel.connectionProvider());
		deptModel.sort().ascending(Department.NAME);

		return deptModel;
	}

	@Override
	protected SwingEntityTableModel createTableModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		return createTableModel(createEditModel(entityType, connectionProvider));
	}

	@Override
	protected SwingEntityTableModel createTableModel(SwingEntityEditModel editModel) {
		return new SwingEntityTableModel(editModel);
	}

	@Override
	protected SwingEntityEditModel createEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		return new SwingEntityEditModel(entityType, connectionProvider);
	}


	@Test
	void nullConditionModel() {
		assertThrows(NullPointerException.class, () -> new SwingEntityTableModel(Employee.TYPE, null));
	}


	@Test
	void getValueAt() {
		testModel.items().refresh();
		assertEquals(1, testModel.getValueAt(0, 0));
		assertEquals(2, testModel.getValueAt(1, 0));
		assertEquals(3, testModel.getValueAt(2, 0));
		assertEquals(4, testModel.getValueAt(3, 0));
		assertEquals(5, testModel.getValueAt(4, 0));
		assertEquals("a", testModel.getValueAt(0, 2));
		assertEquals("b", testModel.getValueAt(1, 2));
		assertEquals("c", testModel.getValueAt(2, 2));
		assertEquals("d", testModel.getValueAt(3, 2));
		assertEquals("e", testModel.getValueAt(4, 2));
	}

	@Test
	void editable() {
		testModel.rowEditor().enabled().set(true);
		assertTrue(testModel.isCellEditable(0, 0));
		assertFalse(testModel.isCellEditable(0, testModel.columns().identifiers().indexOf(Detail.INT_DERIVED)));
		testModel.rowEditor().enabled().set(false);
	}

	@Test
	void setValueAt() {
		SwingEntityTableModel tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		assertThrows(IllegalStateException.class, () -> tableModel.setValueAt("newname", 0, 1));
		tableModel.rowEditor().enabled().set(true);
		tableModel.setValueAt("newname", 0, 1);
		Entity entity = tableModel.items().included().get(0);
		assertEquals("newname", entity.get(Employee.NAME));
		assertThrows(RuntimeException.class, () -> tableModel.setValueAt("newname", 0, 0));
	}

	@Test
	void getColumnClass() {
		assertEquals(Integer.class, testModel.getColumnClass(0));
		assertEquals(Double.class, testModel.getColumnClass(1));
		assertEquals(String.class, testModel.getColumnClass(2));
		assertEquals(LocalDate.class, testModel.getColumnClass(3));
		assertEquals(LocalDateTime.class, testModel.getColumnClass(4));
		assertEquals(Boolean.class, testModel.getColumnClass(5));
		assertEquals(Boolean.class, testModel.getColumnClass(6));
		assertEquals(Entity.class, testModel.getColumnClass(7));
	}






	@Test
	void orderQuery() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, testModel.connectionProvider());
		OrderBy orderBy = tableModel.query().orderBy().getOrThrow();
		//default order by for entity
		assertEquals(2, orderBy.orderByColumns().size());
		assertTrue(orderBy.orderByColumns().get(0).ascending());
		assertEquals(Employee.DEPARTMENT, orderBy.orderByColumns().get(0).column());
		assertTrue(orderBy.orderByColumns().get(1).ascending());
		assertEquals(Employee.NAME, orderBy.orderByColumns().get(1).column());

		tableModel.sort().ascending(Employee.NAME);

		orderBy = tableModel.query().orderBy().getOrThrow();
		//still default order by for entity
		assertEquals(2, orderBy.orderByColumns().size());
		assertTrue(orderBy.orderByColumns().get(0).ascending());
		assertEquals(Employee.DEPARTMENT, orderBy.orderByColumns().get(0).column());
		assertTrue(orderBy.orderByColumns().get(1).ascending());
		assertEquals(Employee.NAME, orderBy.orderByColumns().get(1).column());

		tableModel.orderQuery().set(true);
		orderBy = tableModel.query().orderBy().getOrThrow();
		assertEquals(1, orderBy.orderByColumns().size());
		assertTrue(orderBy.orderByColumns().get(0).ascending());
		assertEquals(Employee.NAME, orderBy.orderByColumns().get(0).column());

		tableModel.sort().order(Employee.HIREDATE).set(SortOrder.DESCENDING);
		tableModel.sort().order(Employee.NAME).add(SortOrder.ASCENDING);

		orderBy = tableModel.query().orderBy().getOrThrow();
		assertEquals(2, orderBy.orderByColumns().size());
		assertFalse(orderBy.orderByColumns().get(0).ascending());
		assertEquals(Employee.HIREDATE, orderBy.orderByColumns().get(0).column());
		assertTrue(orderBy.orderByColumns().get(1).ascending());
		assertEquals(Employee.NAME, orderBy.orderByColumns().get(1).column());

		tableModel.sort().clear();
		orderBy = tableModel.query().orderBy().getOrThrow();
		//back to default order by for entity
		assertEquals(2, orderBy.orderByColumns().size());
		assertTrue(orderBy.orderByColumns().get(0).ascending());
		assertEquals(Employee.DEPARTMENT, orderBy.orderByColumns().get(0).column());
		assertTrue(orderBy.orderByColumns().get(1).ascending());
		assertEquals(Employee.NAME, orderBy.orderByColumns().get(1).column());
	}


//	@Test
//	void replacePerformance() {
//		Entities entities = testModel.connectionProvider().entities();
//		List<Entity> items = IntStream.range(0, 100_000)
//						.mapToObj(i -> entities.entity(Department.TYPE)
//										.with(Department.ID, i)
//										.with(Department.NAME, "dept" + i)
//										.build())
//						.toList();
//		SwingEntityTableModel tableModel = new SwingEntityTableModel(Department.TYPE, testModel.connectionProvider()) {
//			@Override
//			protected Collection<Entity> refreshItems() {
//				return items;
//			}
//		};
//		tableModel.refresh();
//		Random random = new Random();
//		List<Entity> listItems = new ArrayList<>(tableModel.includedItems());
//		while (true) {
//			List<Entity> toReplace = IntStream.range(0, 1000)
//							.mapToObj(i -> listItems.remove(random.nextInt(100_000 - i)))
//							.toList();
//			long millis = System.currentTimeMillis();
//			tableModel.replace(toReplace);
//			System.out.println(System.currentTimeMillis() - millis);
//			tableModel.refresh();
//			listItems.clear();
//			listItems.addAll(tableModel.visibleItems());
//		}
//	}
}
