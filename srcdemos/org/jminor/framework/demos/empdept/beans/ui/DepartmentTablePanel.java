/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.ui.EntityTablePanel;
import org.jminor.framework.client.ui.reporting.EntityReportUiUtil;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.plugins.jasperreports.model.JasperReportsWrapper;
import org.jminor.framework.plugins.jasperreports.ui.JasperReportsUIWrapper;

import java.util.Collection;
import java.util.HashMap;

public class DepartmentTablePanel extends EntityTablePanel {

  public DepartmentTablePanel(final EntityTableModel tableModel) {
    super(tableModel);
  }

  public void viewEmployeeReport() throws Exception {
    if (getTableModel().isSelectionEmpty()) {
      return;
    }

    final String reportPath = Configuration.getReportPath() + "/empdept_employees.jasper";
    final Collection<Object> departmentNumbers =
            EntityUtil.getDistinctPropertyValues(getTableModel().getSelectedItems(), EmpDept.DEPARTMENT_ID);
    final HashMap<String, Object> reportParameters = new HashMap<String, Object>();
    reportParameters.put("DEPTNO", departmentNumbers);
    EntityReportUiUtil.viewJdbcReport(this, new JasperReportsWrapper(reportPath, reportParameters),
            new JasperReportsUIWrapper(), null, getTableModel().getDbProvider());
  }

  @Override
  protected ControlSet getPopupControls(final ControlSet additionalPopupControls) {
    final ControlSet controlSet = super.getPopupControls(additionalPopupControls);
    final ControlSet printControlSet = new ControlSet(Messages.get(Messages.PRINT));
    controlSet.add(ControlFactory.methodControl(this, "viewEmployeeReport", EmpDept.getString(EmpDept.EMPLOYEE_REPORT)));
    controlSet.addAt(printControlSet, 0);

    return controlSet;
  }
}
