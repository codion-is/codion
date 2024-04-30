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
package is.codion.framework.demos.employees.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import java.awt.Dimension;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static is.codion.swing.framework.ui.EntityTablePanel.EntityTablePanelControl.PRINT;

// tag::constructor[]
public class DepartmentTablePanel extends EntityTablePanel {

	public DepartmentTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel);
	}
	// end::constructor[]

	// tag::setupControls[]
	@Override
	protected void setupControls() {
		control(PRINT).set(Control.builder(this::viewEmployeeReport)
						.name("Employee Report")
						.enabled(tableModel().selectionModel().selectionNotEmpty())
						.build());
	}
	// end::setupControls[]

	// tag::viewEmployeeReport[]
	private void viewEmployeeReport() throws ReportException, DatabaseException {
		Collection<Integer> departmentNumbers =
						Entity.distinct(Department.DEPARTMENT_NO,
										tableModel().selectionModel().getSelectedItems());
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", departmentNumbers);

		JasperPrint employeeReport = tableModel().connection()
						.report(Employee.EMPLOYEE_REPORT, reportParameters);

		Dialogs.componentDialog(new JRViewer(employeeReport))
						.owner(this)
						.modal(false)
						.size(new Dimension(800, 600))
						.show();
	}
	// end::viewEmployeeReport[]
}