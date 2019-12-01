/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.ui;

import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.plugin.jasperreports.model.JasperReportsWrapper;
import org.jminor.plugin.jasperreports.ui.JasperReportsUIWrapper;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityTablePanel;
import org.jminor.swing.framework.ui.reporting.EntityReportUiUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerTablePanel extends EntityTablePanel {

  public CustomerTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected ControlSet getPrintControls() {
    ControlSet printControls = super.getPrintControls();
    //add a Control which calls the viewCustomerReport method in this class
    //enabled only when the selection is not empty
    printControls.add(Controls.control(this::viewCustomerReport, "Customer report",
            getTableModel().getSelectionModel()
                    .getSelectionEmptyObserver().getReversedObserver()));

    return printControls;
  }

  private void viewCustomerReport() throws Exception {
    List<Entity> selectedCustomers = getTableModel().getSelectionModel().getSelectedItems();
    if (selectedCustomers.isEmpty()) {
      return;
    }

    String reportPath = "http://test.io/customer_report.jasper";
    Collection<Integer> customerIds = Entities.getValues(Store.CUSTOMER_ID, selectedCustomers);
    Map<String, Object> reportParameters = new HashMap<String, Object>();
    reportParameters.put("CUSTOMER_IDS", customerIds);

    EntityReportUiUtil.viewJdbcReport(this,
            new JasperReportsWrapper(reportPath, reportParameters),
            new JasperReportsUIWrapper(),  "Customer Report",
            getEntityTableModel().getConnectionProvider());
  }
}
