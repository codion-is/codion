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
package is.codion.demos.employees.ui;

import is.codion.demos.employees.domain.Employees.Employee;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableCellRenderer;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.awt.Color;

// tag::employeeTablePanel[]
public class EmployeeTablePanel extends EntityTablePanel {

	public EmployeeTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						.cellRenderer(Employee.JOB, EntityTableCellRenderer.builder(Employee.JOB, tableModel)
										.background((table, employee, attribute, job) ->
														"Manager".equals(job) ? Color.CYAN : null)
										.build())
						.cellRenderer(Employee.SALARY, EntityTableCellRenderer.builder(Employee.SALARY, tableModel)
										.foreground((table, employee, attribute, salary) ->
														salary.doubleValue() < 1300 ? Color.RED : null)
										.build()));
	}
}
// end::employeeTablePanel[]
