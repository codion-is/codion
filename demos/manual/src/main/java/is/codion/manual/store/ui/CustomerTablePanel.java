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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.manual.store.ui;

import is.codion.framework.domain.entity.Entity;
import is.codion.manual.store.domain.Store.Customer;
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

import static is.codion.swing.framework.ui.EntityTablePanel.ControlKeys.PRINT;

// tag::customerTablePanel[]
public class CustomerTablePanel extends EntityTablePanel {

	public CustomerTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel);
		// associate a custom Control with the PRINT control key,
		// which calls the viewCustomerReport method in this class,
		// enabled only when the selection is not empty
		control(PRINT).set(Control.builder()
						.command(this::viewCustomerReport)
						.caption("Customer report")
						.smallIcon(FrameworkIcons.instance().print())
						.enabled(tableModel().selection().empty().not())
						.build());
	}

	private void viewCustomerReport() {
		List<Entity> selectedCustomers = tableModel().selection().items().get();
		if (selectedCustomers.isEmpty()) {
			return;
		}

		Collection<String> customerIds = Entity.values(Customer.ID, selectedCustomers);
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("CUSTOMER_IDS", customerIds);

		JasperPrint customerReport = tableModel().connection()
						.report(Customer.REPORT, reportParameters);

		Dialogs.dialog()
						.component(new JRViewer(customerReport))
						.owner(this)
						.modal(false)
						.title("Customer Report")
						.size(new Dimension(800, 600))
						.show();
	}
}
// end::customerTablePanel[]