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
package is.codion.framework.demos.employees.ui;

import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableCellRendererFactory;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.awt.Color;

public class EmployeeTablePanel extends EntityTablePanel {

	public EmployeeTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						.configureTable(builder -> builder
										.cellRendererFactory(new EmployeeCellRendererFactory(tableModel))));
	}

	private static class EmployeeCellRendererFactory extends EntityTableCellRendererFactory {

		private EmployeeCellRendererFactory(SwingEntityTableModel tableModel) {
			super(tableModel);
		}

		@Override
		public FilterTableCellRenderer create(Attribute<?> attribute) {
			if (attribute.equals(Employee.JOB)) {
				return builder(Employee.JOB)
								.background((table, row, value) -> {
									if ("Manager".equals(value)) {
										return Color.CYAN;
									}

									return null;
								})
								.build();
			}
			if (attribute.equals(Employee.SALARY)) {
				return builder(Employee.SALARY)
								.foreground((table, row, value) -> {
									double salary = value.doubleValue();
									if (salary < 1300) {
										return Color.RED;
									}

									return null;
								})
								.build();
			}

			return super.create(attribute);
		}
	}
}
