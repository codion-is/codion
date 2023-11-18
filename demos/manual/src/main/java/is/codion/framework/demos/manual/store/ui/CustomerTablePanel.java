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
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store.Customer;
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
import java.util.List;
import java.util.Map;

// tag::customerTablePanel[]
public class CustomerTablePanel extends EntityTablePanel {

  public CustomerTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    // associate a custom Control with the PRINT control code,
    // which calls the viewCustomerReport method in this class,
    // enabled only when the selection is not empty
    setControl(ControlCode.PRINT, Control.builder(this::viewCustomerReport)
            .name("Customer report")
            .smallIcon(FrameworkIcons.instance().print())
            .enabled(tableModel().selectionModel().selectionNotEmpty())
            .build());
  }

  private void viewCustomerReport() throws Exception {
    List<Entity> selectedCustomers = tableModel().selectionModel().getSelectedItems();
    if (selectedCustomers.isEmpty()) {
      return;
    }

    Collection<String> customerIds = Entity.values(Customer.ID, selectedCustomers);
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIds);

    JasperPrint customerReport = tableModel().connectionProvider().connection()
            .report(Customer.REPORT, reportParameters);

    Dialogs.componentDialog(new JRViewer(customerReport))
            .owner(this)
            .modal(false)
            .title("Customer Report")
            .size(new Dimension(800, 600))
            .show();
  }
}
// end::customerTablePanel[]