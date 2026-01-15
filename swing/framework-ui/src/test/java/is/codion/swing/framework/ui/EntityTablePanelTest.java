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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.TestDomain.Detail;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntityTablePanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	@Test
	void excludeHiddenColumns() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		tableModel.items().refresh();
		tableModel.items().get().forEach(employee -> {
			assertTrue(employee.contains(Employee.ID));
			assertTrue(employee.contains(Employee.NAME));
			assertTrue(employee.contains(Employee.COMMISSION));
			assertTrue(employee.contains(Employee.DEPARTMENT));
			assertTrue(employee.contains(Employee.HIREDATE));
			assertTrue(employee.contains(Employee.JOB));
		});
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.excludeHiddenColumns(true));
		tablePanel.table().columnModel().visible().set(Employee.ID, Employee.NAME, Employee.COMMISSION);
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
		assertEquals(14, tablePanel.table().columnModel().getColumnIndex(Detail.INT_ITEMS));
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
	void editableAttributesExcludesDerivedAndDenormalizedAttributes() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, CONNECTION_PROVIDER);
		new EntityTablePanel(tableModel, config -> config.editable(attributes -> {
			assertEquals(14, attributes.size());
			assertFalse(attributes.contains(Detail.MASTER_NAME));
			assertFalse(attributes.contains(Detail.MASTER_CODE));
			assertFalse(attributes.contains(Detail.INT_DERIVED));
		}));
	}
}
