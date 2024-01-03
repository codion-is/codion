/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import java.awt.Dimension;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public final class CustomerTablePanel extends EntityTablePanel {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(CustomerTablePanel.class.getName());

  public CustomerTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    setRefreshButtonVisible(RefreshButtonVisible.ALWAYS);
  }

  @Override
  protected void setupControls() {
    control(TableControl.PRINT).set(Control.builder(this::viewCustomerReport)
            .name(BUNDLE.getString("customer_report"))
            .smallIcon(FrameworkIcons.instance().print())
            .enabled(tableModel().selectionModel().selectionNotEmpty())
            .build());
  }

  private void viewCustomerReport() {
    Dialogs.progressWorkerDialog(this::fillCustomerReport)
            .onResult(this::viewReport)
            .execute();
  }

  private JasperPrint fillCustomerReport() throws DatabaseException, ReportException {
    Collection<Long> customerIDs = Entity.values(Customer.ID,
            tableModel().selectionModel().getSelectedItems());
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIDs);

    return tableModel().connectionProvider().connection().report(Customer.REPORT, reportParameters);
  }

  private void viewReport(JasperPrint customerReport) {
    Dialogs.componentDialog(new JRViewer(customerReport))
            .owner(this)
            .modal(false)
            .title(BUNDLE.getString("customer_report"))
            .size(new Dimension(800, 600))
            .show();
  }
}