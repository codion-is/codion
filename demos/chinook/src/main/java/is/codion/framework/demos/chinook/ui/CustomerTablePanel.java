/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityReports;
import is.codion.swing.framework.ui.EntityTablePanel;

import net.sf.jasperreports.swing.JRViewer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CustomerTablePanel extends EntityTablePanel {

  public CustomerTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected Controls createPrintControls() {
    final Controls printControls = super.createPrintControls();
    printControls.add(Control.builder()
            .command(this::viewCustomerReport)
            .name("Customer report")
            .enabledState(getTable().getModel().getSelectionModel().getSelectionNotEmptyObserver())
            .build());

    return printControls;
  }

  private void viewCustomerReport() throws Exception {
    final Collection<Long> customerIDs = Entity.getDistinct(Customer.ID,
            getTableModel().getSelectionModel().getSelectedItems());
    final Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIDs);

    EntityReports.viewJdbcReport(CustomerTablePanel.this, Customer.REPORT,
            reportParameters, JRViewer::new, null, getTableModel().getConnectionProvider());
  }
}