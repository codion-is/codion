/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.state.StateObserver;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.framework.domain.entity.Entities;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.common.ui.control.Controls;
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
            Entities.getDistinctValues(EmpDept.DEPARTMENT_ID,
                    getTableModel().getSelectionModel().getSelectedItems());
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", departmentNumbers);
    EntityReports.viewJdbcReport(DepartmentTablePanel.this, EmpDept.EMPLOYEE_REPORT,
            reportParameters, JRViewer::new, "Employee Report", getTableModel().getConnectionProvider());
  }
  // end::viewEmployeeReport[]

  // tag::getPrintControls[]
  @Override
  protected ControlList getPrintControls() {
    final ControlList printControls = super.getPrintControls();
    final StateObserver selectionNotEmptyObserver =
            getTableModel().getSelectionModel().getSelectionNotEmptyObserver();
    printControls.add(Controls.control(this::viewEmployeeReport,
            "Employee Report", selectionNotEmptyObserver));

    return printControls;
  }
}
// end::getPrintControls[]