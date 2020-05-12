/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.ui;

import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.entity.Entities;
import org.jminor.swing.common.ui.control.ControlList;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityReports;
import org.jminor.swing.framework.ui.EntityTablePanel;

import net.sf.jasperreports.swing.JRViewer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.jminor.framework.demos.chinook.domain.Chinook.CUSTOMER_CUSTOMERID;

public class CustomerTablePanel extends EntityTablePanel {

  public CustomerTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected ControlList getPrintControls() {
    final ControlList printControls = super.getPrintControls();
    printControls.add(Controls.control(this::viewCustomerReport, "Customer report",
            getTable().getModel().getSelectionModel().getSelectionNotEmptyObserver()));

    return printControls;
  }

  private void viewCustomerReport() throws Exception {
    final Collection<Integer> customerIDs = Entities.getDistinctValues(CUSTOMER_CUSTOMERID,
            getTableModel().getSelectionModel().getSelectedItems());
    final Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIDs);

    EntityReports.viewJdbcReport(CustomerTablePanel.this, Chinook.CUSTOMER_REPORT,
            reportParameters, JRViewer::new, null, getTableModel().getConnectionProvider());
  }
}