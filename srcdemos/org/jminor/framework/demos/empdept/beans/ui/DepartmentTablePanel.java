/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.swing.ui.control.ControlSet;
import org.jminor.common.swing.ui.control.Controls;
import org.jminor.framework.Configuration;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.plugins.jasperreports.model.JasperReportsWrapper;
import org.jminor.framework.plugins.jasperreports.ui.JasperReportsUIWrapper;
import org.jminor.framework.swing.model.EntityTableModel;
import org.jminor.framework.swing.ui.EntityTablePanel;
import org.jminor.framework.swing.ui.reporting.EntityReportUiUtil;

import javax.swing.SwingWorker;
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
    new SwingWorker() {
      @Override
      protected Object doInBackground() throws Exception {
        EntityReportUiUtil.viewJdbcReport(DepartmentTablePanel.this, new JasperReportsWrapper(reportPath, reportParameters),
                new JasperReportsUIWrapper(), null, getEntityTableModel().getConnectionProvider());
        return null;
      }
    }.execute();
  }

  @Override
  protected ControlSet getPrintControls() {
    final ControlSet printControlSet = super.getPrintControls();
    printControlSet.add(Controls.methodControl(this, "viewEmployeeReport", EmpDept.getString(EMPLOYEE_REPORT)));

    return printControlSet;
  }
}
