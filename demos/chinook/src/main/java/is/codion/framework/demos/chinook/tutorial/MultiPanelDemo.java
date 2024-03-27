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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.tutorial;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Employee;
import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.framework.demos.chinook.ui.EmployeeEditPanel;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.JPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

/**
 * Just a little demo showcasing how a single {@link SwingEntityModel} behaves
 * when used by multiple {@link EntityPanel}s.
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class MultiPanelDemo {

	public static void main(String[] args) {
		Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
		Database.DATABASE_INIT_SCRIPTS.set("src/main/sql/create_schema.sql");

		LocalEntityConnectionProvider connectionProvider = LocalEntityConnectionProvider.builder()
						.domain(new ChinookImpl())
						.user(User.parse("scott:tiger"))
						.build();

		SwingEntityModel employeeModel = new SwingEntityModel(Employee.TYPE, connectionProvider);
		employeeModel.tableModel().refresh();

		JPanel basePanel = new JPanel(gridLayout(2, 2));
		for (int i = 0; i < 4; i++) {
			EntityPanel employeePanel = new EntityPanel(employeeModel, new EmployeeEditPanel(employeeModel.editModel()));
			employeePanel.tablePanel().conditionPanelVisible().set(true);
			employeePanel.initialize();
			basePanel.add(employeePanel);
		}

		Dialogs.componentDialog(basePanel)
						.title("Multi Panel Demo")
						.disposeOnEscape(false)
						.show()
						.dispose();
	}
}
