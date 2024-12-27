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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.UserPreferences;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.ConditionModel.Wildcard;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.TestDomain.Detail;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;

import static org.junit.jupiter.api.Assertions.*;

public class EntityTablePanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	@Test
	void queryHiddenColumns() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		tableModel.items().refresh();
		tableModel.items().get().forEach(employee -> {
			assertTrue(employee.contains(Employee.ID));
			assertTrue(employee.contains(Employee.NAME));
			assertTrue(employee.contains(Employee.COMMISSION));
			assertTrue(employee.contains(Employee.DEPARTMENT));
			assertTrue(employee.contains(Employee.HIREDATE));
			assertTrue(employee.contains(Employee.JOB));
		});
		tablePanel.table().columnModel().visible().set(Employee.ID, Employee.NAME, Employee.COMMISSION);
		tablePanel.queryHiddenColumns().set(false);
		tableModel.items().refresh();
		tableModel.items().get().forEach(employee -> {
			assertTrue(employee.contains(Employee.ID));
			assertTrue(employee.contains(Employee.NAME));
			assertTrue(employee.contains(Employee.COMMISSION));
			assertFalse(employee.contains(Employee.DEPARTMENT_FK));
			assertFalse(employee.contains(Employee.HIREDATE));
			assertFalse(employee.contains(Employee.JOB));
		});
	}

	@Test
	void getColumnIndex() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		assertEquals(0, tablePanel.table().columnModel().getColumnIndex(Detail.INT));
		assertEquals(1, tablePanel.table().columnModel().getColumnIndex(Detail.DOUBLE));
		assertEquals(2, tablePanel.table().columnModel().getColumnIndex(Detail.BIG_DECIMAL));
		assertEquals(3, tablePanel.table().columnModel().getColumnIndex(Detail.STRING));
		assertEquals(4, tablePanel.table().columnModel().getColumnIndex(Detail.DATE));
		assertEquals(5, tablePanel.table().columnModel().getColumnIndex(Detail.TIME));
		assertEquals(6, tablePanel.table().columnModel().getColumnIndex(Detail.TIMESTAMP));
		assertEquals(7, tablePanel.table().columnModel().getColumnIndex(Detail.OFFSET));
		assertEquals(8, tablePanel.table().columnModel().getColumnIndex(Detail.BOOLEAN));
		assertEquals(9, tablePanel.table().columnModel().getColumnIndex(Detail.BOOLEAN_NULLABLE));
		assertEquals(10, tablePanel.table().columnModel().getColumnIndex(Detail.MASTER_FK));
		assertEquals(11, tablePanel.table().columnModel().getColumnIndex(Detail.DETAIL_FK));
		assertEquals(12, tablePanel.table().columnModel().getColumnIndex(Detail.MASTER_NAME));
		assertEquals(13, tablePanel.table().columnModel().getColumnIndex(Detail.MASTER_CODE));
		assertEquals(14, tablePanel.table().columnModel().getColumnIndex(Detail.INT_VALUE_LIST));
		assertEquals(15, tablePanel.table().columnModel().getColumnIndex(Detail.INT_DERIVED));
	}

	@Test
	void columnModel() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumn<Attribute<?>> column = tablePanel.table().columnModel().column(Detail.STRING);
		assertEquals(Detail.STRING, column.identifier());
	}

	@Test
	void preferences() throws BackingStoreException {
		List<Entity> testEntities = initTestEntities(CONNECTION_PROVIDER.entities());
		SwingEntityTableModel testModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(testModel);
		tablePanel.clearPreferences();
		FilterTableColumnModel<Attribute<?>> columnModel = tablePanel.table().columnModel();
		assertTrue(columnModel.visible(Detail.STRING).get());

		columnModel.visible(Detail.STRING).set(false);
		columnModel.moveColumn(1, 0);//double to 0, int to 1
		TableColumn column = columnModel.getColumn(3);
		column.setWidth(150);//timestamp
		column = columnModel.getColumn(5);
		column.setWidth(170);//entity_ref
		ConditionModel<String> condition =
						testModel.queryModel().conditions().attribute(Detail.STRING);
		condition.autoEnable().set(false);
		condition.wildcard().set(Wildcard.PREFIX);
		condition.caseSensitive().set(false);

		tablePanel.savePreferences();

		tablePanel = new EntityTablePanel(testModel);
		tablePanel.applyPreferences();

		columnModel = tablePanel.table().columnModel();
		assertFalse(columnModel.visible(Detail.STRING).get());
		assertEquals(0, columnModel.getColumnIndex(Detail.DOUBLE));
		assertEquals(1, columnModel.getColumnIndex(Detail.INT));
		column = columnModel.getColumn(3);
		assertEquals(150, column.getPreferredWidth());

		column = columnModel.getColumn(5);
		assertEquals(170, column.getPreferredWidth());
		condition = testModel.queryModel().conditions().attribute(Detail.STRING);
		assertFalse(condition.autoEnable().get());
		assertEquals(Wildcard.PREFIX, condition.wildcard().get());
		assertFalse(condition.caseSensitive().get());

		tablePanel.clearPreferences();
		UserPreferences.flushUserPreferences();
	}

	@Test
	void orderQueryBySortOrder() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		OrderBy orderBy = tableModel.queryModel().orderBy().getOrThrow();
		//default order by for entity
		assertEquals(2, orderBy.orderByColumns().size());
		assertTrue(orderBy.orderByColumns().get(0).ascending());
		assertEquals(Employee.DEPARTMENT, orderBy.orderByColumns().get(0).column());
		assertTrue(orderBy.orderByColumns().get(1).ascending());
		assertEquals(Employee.NAME, orderBy.orderByColumns().get(1).column());

		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		tablePanel.table().model().sort().ascending(Employee.NAME);

		orderBy = tableModel.queryModel().orderBy().getOrThrow();
		//still default order by for entity
		assertEquals(2, orderBy.orderByColumns().size());
		assertTrue(orderBy.orderByColumns().get(0).ascending());
		assertEquals(Employee.DEPARTMENT, orderBy.orderByColumns().get(0).column());
		assertTrue(orderBy.orderByColumns().get(1).ascending());
		assertEquals(Employee.NAME, orderBy.orderByColumns().get(1).column());

		tablePanel.orderQueryBySortOrder().set(true);
		orderBy = tableModel.queryModel().orderBy().getOrThrow();
		assertEquals(1, orderBy.orderByColumns().size());
		assertTrue(orderBy.orderByColumns().get(0).ascending());
		assertEquals(Employee.NAME, orderBy.orderByColumns().get(0).column());

		tablePanel.table().model().sort().order(Employee.HIREDATE).set(SortOrder.DESCENDING);
		tablePanel.table().model().sort().order(Employee.NAME).add(SortOrder.ASCENDING);

		orderBy = tableModel.queryModel().orderBy().getOrThrow();
		assertEquals(2, orderBy.orderByColumns().size());
		assertFalse(orderBy.orderByColumns().get(0).ascending());
		assertEquals(Employee.HIREDATE, orderBy.orderByColumns().get(0).column());
		assertTrue(orderBy.orderByColumns().get(1).ascending());
		assertEquals(Employee.NAME, orderBy.orderByColumns().get(1).column());

		tablePanel.table().model().sort().clear();
		orderBy = tableModel.queryModel().orderBy().getOrThrow();
		//back to default order by for entity
		assertEquals(2, orderBy.orderByColumns().size());
		assertTrue(orderBy.orderByColumns().get(0).ascending());
		assertEquals(Employee.DEPARTMENT, orderBy.orderByColumns().get(0).column());
		assertTrue(orderBy.orderByColumns().get(1).ascending());
		assertEquals(Employee.NAME, orderBy.orderByColumns().get(1).column());
	}

	@Test
	void cellRenderers() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		tableModel.items().refresh();
		FilterTableCellRenderer<String> nameRenderer = EntityTableCellRenderer.builder(Employee.NAME, tableModel).build();
		FilterTableCellRenderer<Double> commissionRenderer = EntityTableCellRenderer.builder(Employee.COMMISSION, tableModel).build();
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config
						.cellRenderer(Employee.NAME, nameRenderer)
						.cellRendererFactory((attribute, model) -> {
							if (attribute.equals(Employee.COMMISSION)) {
								return commissionRenderer;
							}

							return EntityTableCellRenderer.builder(attribute, model).build();
						}));
		assertSame(nameRenderer, tablePanel.table().columnModel().column(Employee.NAME).getCellRenderer());
		assertSame(commissionRenderer, tablePanel.table().columnModel().column(Employee.COMMISSION).getCellRenderer());
	}

	private static List<Entity> initTestEntities(Entities entities) {
		List<Entity> testEntities = new ArrayList<>(5);
		String[] stringValues = new String[] {"a", "b", "c", "d", "e"};
		for (int i = 0; i < 5; i++) {
			testEntities.add(entities.builder(Detail.TYPE)
							.with(Detail.ID, (long) i + 1)
							.with(Detail.INT, i + 1)
							.with(Detail.STRING, stringValues[i])
							.build());
		}

		return testEntities;
	}
}
