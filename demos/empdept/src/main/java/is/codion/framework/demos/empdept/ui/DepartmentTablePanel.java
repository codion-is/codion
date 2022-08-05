/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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

// tag::constructor[]
public class DepartmentTablePanel extends EntityTablePanel {

  public DepartmentTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
  }
  // end::constructor[]

  // tag::viewEmployeeReport[]
  public void viewEmployeeReport() throws Exception {
    Collection<Integer> departmentNumbers =
            Entity.getDistinct(Department.ID,
                    getTableModel().getSelectionModel().getSelectedItems());
    HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", departmentNumbers);

    JasperPrint employeeReport = getTableModel().getConnectionProvider().connection()
            .fillReport(Employee.EMPLOYEE_REPORT, reportParameters);

    Dialogs.componentDialog(new JRViewer(employeeReport))
            .owner(this)
            .modal(false)
            .size(new Dimension(800, 600))
            .show();
  }
  // end::viewEmployeeReport[]

  // tag::createPrintControls[]
  @Override
  protected Controls createPrintControls() {
    StateObserver selectionNotEmptyObserver =
            getTableModel().getSelectionModel().getSelectionNotEmptyObserver();

    return super.createPrintControls()
            .add(Control.builder(this::viewEmployeeReport)
                    .caption("Employee Report")
                    .enabledState(selectionNotEmptyObserver)
                    .build());
  }
}
// end::createPrintControls[]