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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.employees.ui;

import is.codion.demos.employees.domain.Employees.Department;
import is.codion.demos.employees.domain.Employees.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import java.awt.Dimension;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static is.codion.swing.framework.ui.EntityTablePanel.ControlKeys.PRINT;

// tag::constructor[]
public class DepartmentTablePanel extends EntityTablePanel {

	public DepartmentTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel);
	}
	// end::constructor[]

	// tag::setupControls[]
	@Override
	protected void setupControls() {
		control(PRINT).set(Control.builder()
						.command(this::viewEmployeeReport)
						.caption("Employee Report")
						.smallIcon(FrameworkIcons.instance().print())
						.enabled(tableModel().selection().empty().not())
						.build());
	}
	// end::setupControls[]

	// tag::viewEmployeeReport[]
	private void viewEmployeeReport() {
		Collection<Integer> departmentNumbers =
						Entity.distinct(Department.DEPARTMENT_NO,
										tableModel().selection().items().get());
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", departmentNumbers);

		JasperPrint employeeReport = tableModel().connection()
						.report(Employee.EMPLOYEE_REPORT, reportParameters);

		Dialogs.dialog()
						.component(new JRViewer(employeeReport))
						.owner(this)
						.modal(false)
						.size(new Dimension(800, 600))
						.show();
	}
	// end::viewEmployeeReport[]
}