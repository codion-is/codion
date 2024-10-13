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
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableCellRenderer;
import is.codion.swing.framework.ui.EntityTableCellRenderer.EntityColorProvider;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.awt.Color;
import java.math.BigDecimal;

public class EmployeeTablePanel extends EntityTablePanel {

	public EmployeeTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						.table(builder -> builder
										.cellRenderer(Employee.JOB, EntityTableCellRenderer.builder(Employee.JOB, tableModel)
														.background(new JobBackgroundProvider())
														.build())
										.cellRenderer(Employee.SALARY, EntityTableCellRenderer.builder(Employee.SALARY, tableModel)
														.foreground(new SalaryForegroundProvider())
														.build())));
	}

	private static final class JobBackgroundProvider implements EntityColorProvider<String> {

		@Override
		public Color color(FilterTable<Entity, Attribute<?>> table, int row, int column, String value) {
			if ("Manager".equals(value)) {
				return Color.CYAN;
			}

			return null;
		}
	}

	private static final class SalaryForegroundProvider implements EntityColorProvider<BigDecimal> {

		@Override
		public Color color(FilterTable<Entity, Attribute<?>> table, int row, int column, BigDecimal value) {
			double salary = value.doubleValue();
			if (salary < 1300) {
				return Color.RED;
			}

			return null;
		}
	}
}
