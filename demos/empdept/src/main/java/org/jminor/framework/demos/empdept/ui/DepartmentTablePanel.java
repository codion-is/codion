/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.ui;

import org.jminor.common.state.StateObserver;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.entity.Entities;
import org.jminor.swing.common.ui.control.ControlList;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityReports;
import org.jminor.swing.framework.ui.EntityTablePanel;

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