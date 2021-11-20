/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import java.awt.Dimension;
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
  protected Controls createPrintControls() {
    Controls printControls = super.createPrintControls();
    //add a Control which calls the viewCustomerReport method in this class
    //enabled only when the selection is not empty
    printControls.add(Control.builder(this::viewCustomerReport)
            .caption("Customer report")
            .enabledState(getTable().getModel().getSelectionModel().getSelectionNotEmptyObserver())
            .build());

    return printControls;
  }

  private void viewCustomerReport() throws Exception {
    List<Entity> selectedCustomers = getTable().getModel().getSelectionModel().getSelectedItems();
    if (selectedCustomers.isEmpty()) {
      return;
    }

    Collection<String> customerIds = Entity.get(Customer.ID, selectedCustomers);
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIds);

    JasperPrint customerReport = getTableModel().getConnectionProvider().getConnection()
            .fillReport(Store.CUSTOMER_REPORT, reportParameters);

    Dialogs.componentDialog(new JRViewer(customerReport))
            .owner(this)
            .modal(false)
            .title("Customer Report")
            .preferredSize(new Dimension(800, 600))
            .show();
  }
}
// end::customerTablePanel[]