/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.model.EntityApplicationModel;
import org.jminor.framework.plugins.jasperreports.model.JasperReportsWrapper;
import org.jminor.framework.plugins.jasperreports.ui.JasperReportsUIWrapper;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityTablePanel;
import org.jminor.swing.framework.ui.reporting.EntityReportUiUtil;

import java.util.Collection;
import java.util.HashMap;

import static org.jminor.framework.demos.chinook.domain.Chinook.CUSTOMER_CUSTOMERID;

public class CustomerTablePanel extends EntityTablePanel {

  public CustomerTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  public void viewCustomerReport() throws Exception {
    if (getEntityTableModel().getSelectionModel().isSelectionEmpty()) {
      return;
    }

    final String reportPath = EntityApplicationModel.getReportPath() + "/customer_report.jasper";
    final Collection customerIDs =
            EntityUtil.getDistinctValues(CUSTOMER_CUSTOMERID, getEntityTableModel().getSelectionModel().getSelectedItems());
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIDs);
    EntityReportUiUtil.viewJdbcReport(CustomerTablePanel.this, new JasperReportsWrapper(reportPath, reportParameters),
            new JasperReportsUIWrapper(), null, getEntityTableModel().getConnectionProvider());
  }

  @Override
  protected ControlSet getPrintControls() {
    final ControlSet printControlSet = super.getPrintControls();
    printControlSet.add(Controls.control(this::viewCustomerReport, "Customer report"));

    return printControlSet;
  }
}