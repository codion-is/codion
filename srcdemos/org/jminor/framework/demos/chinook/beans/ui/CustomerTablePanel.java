/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.Controls;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.ui.EntityTablePanel;
import org.jminor.framework.client.ui.reporting.EntityReportUiUtil;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.plugins.jasperreports.model.JasperReportsWrapper;
import org.jminor.framework.plugins.jasperreports.ui.JasperReportsUIWrapper;

import java.util.Collection;
import java.util.HashMap;

public class CustomerTablePanel extends EntityTablePanel {

  public CustomerTablePanel(final EntityTableModel tableModel) {
    super(tableModel);
  }

  public void viewCustomerReport() throws Exception {
    if (getEntityTableModel().isSelectionEmpty()) {
      return;
    }

    final String reportPath = Configuration.getReportPath() + "/customer_report.jasper";
    final Collection<Object> customerIDs =
            EntityUtil.getDistinctPropertyValues(Chinook.CUSTOMER_CUSTOMERID, getEntityTableModel().getSelectedItems());
    final HashMap<String, Object> reportParameters = new HashMap<String, Object>();
    reportParameters.put("CUSTOMER_IDS", customerIDs);
    EntityReportUiUtil.viewJdbcReport(this, new JasperReportsWrapper(reportPath, reportParameters),
            new JasperReportsUIWrapper(), null, getEntityTableModel().getDbProvider());
  }

  @Override
  protected ControlSet getPrintControls() {
    final ControlSet printControlSet = super.getPrintControls();
    printControlSet.add(Controls.methodControl(this, "viewCustomerReport", "Customer report"));

    return printControlSet;
  }
}