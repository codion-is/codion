/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.ui;

import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.common.state.StateObserver;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.entity.Entities;
import org.jminor.plugin.jasperreports.model.JasperReportsWrapper;
import org.jminor.plugin.jasperreports.ui.JasperReportsUIWrapper;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityReports;
import org.jminor.swing.framework.ui.EntityTablePanel;

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
    final String reportPath = ReportWrapper.getReportPath() + "/empdept_employees.jasper";
    final Collection<Integer> departmentNumbers =
            Entities.getDistinctValues(EmpDept.DEPARTMENT_ID,
                    getTableModel().getSelectionModel().getSelectedItems());
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", departmentNumbers);
    EntityReports.viewJdbcReport(DepartmentTablePanel.this,
            new JasperReportsWrapper(reportPath, reportParameters),
            new JasperReportsUIWrapper(), "Employee Report", getTableModel().getConnectionProvider());
  }
// end::viewEmployeeReport[]

// tag::getPrintControls[]
  @Override
  protected ControlSet getPrintControls() {
    final ControlSet printControlSet = super.getPrintControls();
    final StateObserver selectionNotEmptyObserver =
            getTableModel().getSelectionModel().getSelectionNotEmptyObserver();
    printControlSet.add(Controls.control(this::viewEmployeeReport,
            "Employee Report", selectionNotEmptyObserver));

    return printControlSet;
  }
}
// end::getPrintControls[]