/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.state.StateObserver;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.domain.entity.Entities;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityReports;
import is.codion.swing.framework.ui.EntityTablePanel;

import net.sf.jasperreports.swing.JRViewer;

import java.util.Collection;
import java.util.HashMap;

// tag::constructor[]
public class DepartmentTablePanel extends EntityTablePanel {

  public DepartmentTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }
  // end::constructor[]

  // tag::viewEmployeeReport[]
  public void viewEmployeeReport() throws Exception {
    final Collection<Integer> departmentNumbers =
            Entities.getDistinctValues(Department.ID,
                    getTableModel().getSelectionModel().getSelectedItems());
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", departmentNumbers);
    EntityReports.viewJdbcReport(DepartmentTablePanel.this, Employee.EMPLOYEE_REPORT,
            reportParameters, JRViewer::new, "Employee Report", getTableModel().getConnectionProvider());
  }
  // end::viewEmployeeReport[]

  // tag::createPrintControls[]
  @Override
  protected ControlList createPrintControls() {
    final ControlList printControls = super.createPrintControls();
    final StateObserver selectionNotEmptyObserver =
            getTableModel().getSelectionModel().getSelectionNotEmptyObserver();
    printControls.add(Control.builder()
            .command(this::viewEmployeeReport)
            .name("Employee Report")
            .enabledState(selectionNotEmptyObserver).build());

    return printControls;
  }
}
// end::createPrintControls[]