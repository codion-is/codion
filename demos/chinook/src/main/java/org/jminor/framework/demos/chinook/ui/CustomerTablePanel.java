/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.ui;

import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.domain.Entities;
import org.jminor.plugin.jasperreports.model.JasperReportsWrapper;
import org.jminor.plugin.jasperreports.ui.JasperReportsUIWrapper;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityTablePanel;
import org.jminor.swing.framework.ui.reporting.EntityReportUiUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.jminor.framework.demos.chinook.domain.Chinook.CUSTOMER_CUSTOMERID;

public class CustomerTablePanel extends EntityTablePanel {

  public CustomerTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected ControlSet getPrintControls() {
    final ControlSet printControlSet = super.getPrintControls();
    printControlSet.add(Controls.control(this::viewCustomerReport, "Customer report",
            getTable().getModel().getSelectionModel().getSelectionEmptyObserver().getReversedObserver()));

    return printControlSet;
  }

  private void viewCustomerReport() throws Exception {
    final String reportPath = ReportWrapper.getReportPath() + "/customer_report.jasper";
    final Collection<Integer> customerIDs = Entities.getDistinctValues(CUSTOMER_CUSTOMERID,
            getEntityTableModel().getSelectionModel().getSelectedItems());
    final Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIDs);

    EntityReportUiUtil.viewJdbcReport(CustomerTablePanel.this, new JasperReportsWrapper(reportPath, reportParameters),
            new JasperReportsUIWrapper(), null, getEntityTableModel().getConnectionProvider());
  }
}