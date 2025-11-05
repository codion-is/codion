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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.employees.ui;

import is.codion.common.utilities.user.User;
import is.codion.demos.employees.domain.Employees;
import is.codion.demos.employees.domain.Employees.Department;
import is.codion.demos.employees.model.EmployeeEditModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

import org.junit.jupiter.api.Test;

public final class PanelTest {

	private final EntityConnectionProvider connectionProvider =
					LocalEntityConnectionProvider.builder()
									.domain(new Employees())
									.user(User.parse(System.getProperty("codion.test.user")))
									.build();

	@Test
	void department() {
		SwingEntityModel model = new SwingEntityModel(Department.TYPE, connectionProvider);
		new EntityPanel(model, new DepartmentEditPanel(model.editModel()), new DepartmentTablePanel(model.tableModel())).initialize();
	}

	@Test
	void employee() {
		SwingEntityModel model = new SwingEntityModel(new EmployeeEditModel(connectionProvider));
		new EntityPanel(model, new EmployeeEditPanel(model.editModel()), new EmployeeTablePanel(model.tableModel())).initialize();
	}
}
