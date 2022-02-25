/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
import is.codion.framework.demos.chinook.domain.Chinook.Customer;
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
import java.util.Map;
import java.util.ResourceBundle;

public final class CustomerTablePanel extends EntityTablePanel {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(CustomerTablePanel.class.getName());

  public CustomerTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected Controls createPrintControls() {
    Controls printControls = super.createPrintControls();
    printControls.add(Control.builder(this::viewCustomerReport)
            .caption(BUNDLE.getString("customer_report"))
            .enabledState(getTableModel().getSelectionModel().getSelectionNotEmptyObserver())
            .build());

    return printControls;
  }

  private void viewCustomerReport() throws Exception {
    Dialogs.progressWorkerDialog(this::fillCustomerReport)
            .onResult(this::viewReport)
            .execute();
  }

  private JasperPrint fillCustomerReport() throws DatabaseException, ReportException {
    Collection<Long> customerIDs = Entity.get(Customer.ID,
            getTableModel().getSelectionModel().getSelectedItems());
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIDs);

    return getTableModel().getConnectionProvider().getConnection().fillReport(Customer.REPORT, reportParameters);
  }

  private void viewReport(final JasperPrint customerReport) {
    Dialogs.componentDialog(new JRViewer(customerReport))
            .owner(this)
            .modal(false)
            .title(BUNDLE.getString("customer_report"))
            .size(new Dimension(800, 600))
            .show();
  }
}