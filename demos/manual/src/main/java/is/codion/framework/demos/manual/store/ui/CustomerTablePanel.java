/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

  public CustomerTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected Controls createPrintMenuControls() {
    Controls printMenuControls = super.createPrintMenuControls();
    //add a Control which calls the viewCustomerReport method in this class
    //enabled only when the selection is not empty
    printMenuControls.add(Control.builder(this::viewCustomerReport)
            .caption("Customer report")
            .enabledState(tableModel().selectionModel().selectionNotEmptyObserver())
            .build());

    return printMenuControls;
  }

  private void viewCustomerReport() throws Exception {
    List<Entity> selectedCustomers = tableModel().selectionModel().getSelectedItems();
    if (selectedCustomers.isEmpty()) {
      return;
    }

    Collection<String> customerIds = Entity.get(Customer.ID, selectedCustomers);
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIds);

    JasperPrint customerReport = tableModel().connectionProvider().connection()
            .fillReport(Store.CUSTOMER_REPORT, reportParameters);

    Dialogs.componentDialog(new JRViewer(customerReport))
            .owner(this)
            .modal(false)
            .title("Customer Report")
            .size(new Dimension(800, 600))
            .show();
  }
}
// end::customerTablePanel[]