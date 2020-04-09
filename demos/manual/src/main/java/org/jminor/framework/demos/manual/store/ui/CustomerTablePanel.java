/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.ui;

import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.framework.domain.entity.Entities;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.plugin.jasperreports.model.JasperReports;
import org.jminor.plugin.jasperreports.ui.JasperReportsUiWrapper;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityReports;
import org.jminor.swing.framework.ui.EntityTablePanel;

import net.sf.jasperreports.engine.JasperReport;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// tag::customerTablePanel[]
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
            getTable().getModel().getSelectionModel().getSelectionNotEmptyObserver()));

    return printControls;
  }

  private void viewCustomerReport() throws Exception {
    List<Entity> selectedCustomers = getTable().getModel().getSelectionModel().getSelectedItems();
    if (selectedCustomers.isEmpty()) {
      return;
    }

    JasperReport report = JasperReports.loadJasperReport("http://test.io/customer_report.jasper");
    Collection<Integer> customerIds = Entities.getValues(Store.CUSTOMER_ID, selectedCustomers);
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIds);

    EntityReports.viewJdbcReport(this,
            JasperReports.jasperReportsWrapper(report, reportParameters),
            new JasperReportsUiWrapper(),  "Customer Report",
            getTableModel().getConnectionProvider());
  }
}
// end::customerTablePanel[]