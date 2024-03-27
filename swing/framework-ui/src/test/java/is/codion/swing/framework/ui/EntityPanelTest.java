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

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class EntityPanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void test() {
		SwingEntityModel deptModel = new SwingEntityModel(Department.TYPE, CONNECTION_PROVIDER);
		EntityPanel deptPanel = new EntityPanel(deptModel, null, (EntityTablePanel) null);
		assertFalse(deptPanel.containsEditPanel());
		assertThrows(IllegalStateException.class, deptPanel::editPanel);
		assertFalse(deptPanel.containsTablePanel());
		assertThrows(IllegalStateException.class, deptPanel::tablePanel);
		assertNotNull(deptPanel.editModel());
		assertNotNull(deptPanel.tableModel());

		// panel with a default table panel
		deptPanel = new EntityPanel(deptModel, new EntityEditPanel(deptModel.editModel()) {
			@Override
			protected void initializeUI() {}
		});
		deptPanel.initialize();
		assertNotNull(deptPanel.editPanel());
		assertNotNull(deptPanel.tablePanel());

		deptPanel = new EntityPanel(deptModel, new EntityEditPanel(deptModel.editModel()) {
			@Override
			protected void initializeUI() {}
		}, (EntityTablePanel) null);
		deptPanel.initialize();
		assertNotNull(deptPanel.editPanel());
		assertThrows(IllegalStateException.class, deptPanel::tablePanel);
	}

	@Test
	void detailPanels() {
		SwingEntityModel deptModel = new SwingEntityModel(Department.TYPE, CONNECTION_PROVIDER);
		SwingEntityModel empModel = new SwingEntityModel(Employee.TYPE, CONNECTION_PROVIDER);
		deptModel.addDetailModel(empModel);

		EntityPanel deptPanel = new EntityPanel(deptModel);
		EntityPanel empPanel = new EntityPanel(empModel);

		deptPanel.addDetailPanel(empPanel);
		assertThrows(IllegalArgumentException.class, () -> deptPanel.addDetailPanel(empPanel));
		assertNotNull(deptPanel.detailPanel(Employee.TYPE));
		assertEquals(0, deptPanel.activeDetailPanels().size());

		assertSame(deptPanel, empPanel.parentPanel().orElseThrow(IllegalStateException::new));

		// activates the detail panel
		deptPanel.initialize();
		assertThrows(IllegalStateException.class, () -> deptPanel.addDetailPanels(empPanel));
		assertEquals(1, deptPanel.activeDetailPanels().size());

		deptModel.detailModelLink(empModel).active().set(false);
		assertEquals(0, deptPanel.activeDetailPanels().size());
	}
}
