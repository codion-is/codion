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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

public final class EmployeeTablePanel extends EntityTablePanel {

	public EmployeeTablePanel(SwingEntityTableModel tableModel) {
		// We provide a EmployeeEditPanel instance, which is then accessible
		// via double click, popup menu (Add/Edit) or keyboard shortcuts:
		// INSERT to add a new employee or CTRL-INSERT to edit the selected one.
		super(tableModel, new EmployeeEditPanel(tableModel.editModel()));
	}
}
