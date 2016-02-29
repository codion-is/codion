/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.framework.Configuration;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.plugins.jasperreports.model.JasperReportsWrapper;
import org.jminor.framework.plugins.jasperreports.ui.JasperReportsUIWrapper;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.EntityTableModel;
import org.jminor.swing.framework.ui.EntityTablePanel;
import org.jminor.swing.framework.ui.reporting.EntityReportUiUtil;

import java.util.Collection;
import java.util.HashMap;

import static org.jminor.framework.demos.empdept.domain.EmpDept.DEPARTMENT_ID;
import static org.jminor.framework.demos.empdept.domain.EmpDept.EMPLOYEE_REPORT;

public class DepartmentTablePanel extends EntityTablePanel {

  public DepartmentTablePanel(final EntityTableModel tableModel) {
    super(tableModel);
  }

  public void viewEmployeeReport() throws Exception {
    if (getEntityTableModel().getSelectionModel().isSelectionEmpty()) {
      return;
    }

    final String reportPath = Configuration.getReportPath() + "/empdept_employees.jasper";
    final Collection departmentNumbers =
            EntityUtil.getDistinctPropertyValues(DEPARTMENT_ID, getEntityTableModel().getSelectionModel().getSelectedItems());
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", departmentNumbers);
    EntityReportUiUtil.viewJdbcReport(DepartmentTablePanel.this, new JasperReportsWrapper(reportPath, reportParameters),
            new JasperReportsUIWrapper(), null, getEntityTableModel().getConnectionProvider());
  }

  @Override
  protected ControlSet getPrintControls() {
    final ControlSet printControlSet = super.getPrintControls();
    printControlSet.add(Controls.methodControl(this, "viewEmployeeReport", EmpDept.getString(EMPLOYEE_REPORT)));

    return printControlSet;
  }
}
