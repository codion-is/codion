/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.empdept.ui;

import dev.codion.common.state.StateObserver;
import dev.codion.framework.demos.empdept.domain.EmpDept;
import dev.codion.framework.domain.entity.Entities;
import dev.codion.swing.common.ui.control.ControlList;
import dev.codion.swing.common.ui.control.Controls;
import dev.codion.swing.framework.model.SwingEntityTableModel;
import dev.codion.swing.framework.ui.EntityReports;
import dev.codion.swing.framework.ui.EntityTablePanel;

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