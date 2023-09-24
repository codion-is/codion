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
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.state.StateObserver;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import java.awt.Dimension;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// tag::constructor[]
public class DepartmentTablePanel extends EntityTablePanel {

  public DepartmentTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
  }
  // end::constructor[]

  // tag::viewEmployeeReport[]
  public void viewEmployeeReport() throws Exception {
    Collection<Integer> departmentNumbers =
            Entity.distinct(Department.ID,
                    tableModel().selectionModel().getSelectedItems());
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", departmentNumbers);

    JasperPrint employeeReport = tableModel().connectionProvider().connection()
            .fillReport(Employee.EMPLOYEE_REPORT, reportParameters);

    Dialogs.componentDialog(new JRViewer(employeeReport))
            .owner(this)
            .modal(false)
            .size(new Dimension(800, 600))
            .show();
  }
  // end::viewEmployeeReport[]

  // tag::createPrintMenuControls[]
  @Override
  protected Controls createPrintMenuControls() {
    StateObserver selectionNotEmpty =
            tableModel().selectionModel().selectionNotEmpty();

    return super.createPrintMenuControls()
            .add(Control.builder(this::viewEmployeeReport)
                    .name("Employee Report")
                    .enabled(selectionNotEmpty)
                    .build());
  }
}
// end::createPrintMenuControls[]