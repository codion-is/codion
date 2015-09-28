/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.swing.ui.control.ControlSet;
import org.jminor.common.swing.ui.control.Controls;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.ui.EntityTablePanel;
import org.jminor.framework.client.ui.reporting.EntityReportUiUtil;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.plugins.jasperreports.model.JasperReportsWrapper;
import org.jminor.framework.plugins.jasperreports.ui.JasperReportsUIWrapper;

import javax.swing.SwingWorker;
import java.util.Collection;
import java.util.HashMap;

import static org.jminor.framework.demos.chinook.domain.Chinook.CUSTOMER_CUSTOMERID;

public class CustomerTablePanel extends EntityTablePanel {

  public CustomerTablePanel(final EntityTableModel tableModel) {
    super(tableModel);
  }

  public void viewCustomerReport() throws Exception {
    if (getEntityTableModel().getSelectionModel().isSelectionEmpty()) {
      return;
    }

    final String reportPath = Configuration.getReportPath() + "/customer_report.jasper";
    final Collection customerIDs =
            EntityUtil.getDistinctPropertyValues(CUSTOMER_CUSTOMERID, getEntityTableModel().getSelectionModel().getSelectedItems());
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIDs);
    new SwingWorker() {
      @Override
      protected Object doInBackground() throws Exception {
        EntityReportUiUtil.viewJdbcReport(CustomerTablePanel.this, new JasperReportsWrapper(reportPath, reportParameters),
                new JasperReportsUIWrapper(), null, getEntityTableModel().getConnectionProvider());
        return null;
      }
    }.execute();
  }

  @Override
  protected ControlSet getPrintControls() {
    final ControlSet printControlSet = super.getPrintControls();
    printControlSet.add(Controls.methodControl(this, "viewCustomerReport", "Customer report"));

    return printControlSet;
  }
}