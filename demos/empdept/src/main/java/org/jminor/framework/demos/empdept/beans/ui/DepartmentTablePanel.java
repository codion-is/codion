/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.StateObserver;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.model.EntityApplicationModel;
import org.jminor.plugin.jasperreports.model.JasperReportsWrapper;
import org.jminor.plugin.jasperreports.ui.JasperReportsUIWrapper;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityTablePanel;
import org.jminor.swing.framework.ui.reporting.EntityReportUiUtil;

import java.util.Collection;
import java.util.HashMap;

public class DepartmentTablePanel extends EntityTablePanel {

  public DepartmentTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  public void viewEmployeeReport() throws Exception {
    final String reportPath = EntityApplicationModel.getReportPath() + "/empdept_employees.jasper";
    final Collection departmentNumbers =
            Entities.getDistinctValues(EmpDept.DEPARTMENT_ID,
                    getEntityTableModel().getSelectionModel().getSelectedItems());
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", departmentNumbers);
    EntityReportUiUtil.viewJdbcReport(DepartmentTablePanel.this,
            new JasperReportsWrapper(reportPath, reportParameters),
            new JasperReportsUIWrapper(), "Employee Report", getEntityTableModel().getConnectionProvider());
  }

  @Override
  protected ControlSet getPrintControls() {
    final ControlSet printControlSet = super.getPrintControls();
    final StateObserver selectionNotEmptyObserver = getEntityTableModel().getSelectionModel()
            .getSelectionEmptyObserver().getReversedObserver();
    printControlSet.add(Controls.control(this::viewEmployeeReport,
            "Employee Report", selectionNotEmptyObserver));

    return printControlSet;
  }
}
