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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.EntityQueryModel;
import is.codion.framework.model.test.AbstractEntityTableModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityTableModelTest extends AbstractEntityTableModelTest<SwingEntityEditModel, SwingEntityTableModel> {

	@Override
	protected SwingEntityTableModel createTestTableModel() {
		return new SwingEntityTableModel(Detail.TYPE, testEntities, connectionProvider());
	}

	@Override
	protected SwingEntityTableModel createDepartmentTableModel() {
		SwingEntityTableModel deptModel = createTableModel(Department.TYPE, testModel.connectionProvider());
		deptModel.sorter().setSortOrder(Department.NAME, SortOrder.ASCENDING);

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
	void refreshOnForeignKeyConditionValuesSet() {
		SwingEntityTableModel employeeTableModel = createTableModel(Employee.TYPE, connectionProvider());
		assertEquals(0, employeeTableModel.items().visible().count());
		Entity accounting = connectionProvider().connection().selectSingle(Department.ID.equalTo(10));
		employeeTableModel.queryModel().conditions().setInOperands(Employee.DEPARTMENT_FK, singletonList(accounting));
		employeeTableModel.refresh();
		assertEquals(7, employeeTableModel.items().visible().count());
	}

	@Test
	void nullConditionModel() {
		assertThrows(NullPointerException.class, () -> new SwingEntityTableModel(Employee.TYPE, null));
	}

	@Test
	void testFiltering() {
		testModel.refresh();
		ConditionModel<String> filterModel =
						testModel.filters().get(Detail.STRING);
		filterModel.operands().equal().set("a");
		testModel.items().filter();
		assertEquals(4, testModel.items().filtered().count());
	}

	@Test
	void getValueAt() {
		testModel.refresh();
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
		testModel.editable().set(true);
		assertTrue(testModel.isCellEditable(0, 0));
		assertFalse(testModel.isCellEditable(0, testModel.columns().identifiers().indexOf(Detail.INT_DERIVED)));
		testModel.editable().set(false);
	}

	@Test
	void setValueAt() {
		SwingEntityTableModel tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.refresh();
		assertThrows(IllegalStateException.class, () -> tableModel.setValueAt("newname", 0, 1));
		tableModel.editable().set(true);
		tableModel.setValueAt("newname", 0, 1);
		Entity entity = tableModel.items().visible().itemAt(0);
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
	void validItems() {
		SwingEntityTableModel tableModel = createTableModel(Employee.TYPE, connectionProvider());
		Entity dept = tableModel.entities().builder(Department.TYPE)
						.with(Department.ID, 1)
						.with(Department.NAME, "dept")
						.build();
		assertThrows(IllegalArgumentException.class, () -> tableModel.items().addItems(singletonList(dept)));
		assertThrows(IllegalArgumentException.class, () -> tableModel.items().visible().addItemsAt(0, singletonList(dept)));

		assertThrows(NullPointerException.class, () -> tableModel.items().addItems(singletonList(null)));
		assertThrows(NullPointerException.class, () -> tableModel.items().visible().addItemsAt(0, singletonList(null)));
	}

	@Test
	void conditionChanged() {
		SwingEntityTableModel tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.refresh();
		ConditionModel<String> nameCondition = tableModel.queryModel().conditions().get(Employee.NAME);
		nameCondition.operands().equal().set("JONES");
		assertTrue(tableModel.queryModel().conditionChanged().get());
		tableModel.refresh();
		assertFalse(tableModel.queryModel().conditionChanged().get());
		nameCondition.enabled().set(false);
		assertTrue(tableModel.queryModel().conditionChanged().get());
		nameCondition.enabled().set(true);
		assertFalse(tableModel.queryModel().conditionChanged().get());
	}

	@Test
	void isConditionEnabled() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, testModel.connectionProvider());
		EntityQueryModel queryModel = tableModel.queryModel();
		queryModel.conditionEnabled().set(queryModel.conditions().get(Employee.MGR_FK).enabled());
		tableModel.refresh();
		assertEquals(16, tableModel.items().visible().count());
		queryModel.conditionRequired().set(true);
		tableModel.refresh();
		assertEquals(0, tableModel.items().visible().count());
		ConditionModel<Entity> mgrCondition = queryModel.conditions().get(Employee.MGR_FK);
		mgrCondition.operands().equal().set(null);
		mgrCondition.enabled().set(true);
		tableModel.refresh();
		assertEquals(1, tableModel.items().visible().count());
		mgrCondition.enabled().set(false);
		tableModel.refresh();
		assertEquals(0, tableModel.items().visible().count());
	}

	@Test
	void handleEditEvents() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, testModel.connectionProvider());
		tableModel.refresh();
		SwingEntityEditModel employeeEditModel = new SwingEntityEditModel(Employee.TYPE, testModel.connectionProvider());
		employeeEditModel.editor().set(tableModel.items().visible().itemAt(0));
		String newName = "new name";
		employeeEditModel.value(Employee.NAME).set(newName);
		SwingEntityEditModel departmentEditModel = new SwingEntityEditModel(Department.TYPE, testModel.connectionProvider());
		departmentEditModel.editor().set(employeeEditModel.value(Employee.DEPARTMENT_FK).get());
		departmentEditModel.value(Department.NAME).set(newName);
		EntityConnection connection = tableModel.connectionProvider().connection();
		connection.startTransaction();
		try {
			employeeEditModel.update();
			assertEquals(newName, tableModel.items().visible().itemAt(0).get(Employee.NAME));
			departmentEditModel.update();
			assertEquals(newName, tableModel.items().visible().itemAt(0).get(Employee.DEPARTMENT_FK).get(Department.NAME));
		}
		finally {
			connection.rollbackTransaction();
		}
	}

//	@Test
//	void replacePerformance() {
//		Entities entities = testModel.connectionProvider().entities();
//		List<Entity> items = IntStream.range(0, 100_000)
//						.mapToObj(i -> entities.builder(Department.TYPE)
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
//		List<Entity> listItems = new ArrayList<>(tableModel.visibleItems());
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
