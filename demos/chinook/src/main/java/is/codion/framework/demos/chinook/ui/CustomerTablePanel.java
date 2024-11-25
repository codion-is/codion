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

import static is.codion.swing.framework.ui.EntityTablePanel.ControlKeys.PRINT;
import static java.util.ResourceBundle.getBundle;

public final class CustomerTablePanel extends EntityTablePanel {

	private static final ResourceBundle BUNDLE = getBundle(CustomerTablePanel.class.getName());

	public CustomerTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						// Otherwise the table refresh button is only
						// visible when the condition panel is visible
						.refreshButtonVisible(RefreshButtonVisible.ALWAYS));
	}

	@Override
	protected void setupControls() {
		// Assign a custom report action to the standard PRINT control,
		// which is then made available in the popup menu and on the toolbar
		control(PRINT).set(Control.builder()
						.command(this::viewCustomerReport)
						.name(BUNDLE.getString("customer_report"))
						.smallIcon(FrameworkIcons.instance().print())
						.enabled(tableModel().selection().empty().not())
						.build());
	}

	private void viewCustomerReport() {
		Dialogs.progressWorkerDialog(this::fillCustomerReport)
						.owner(this)
						.title(BUNDLE.getString("customer_report"))
						.onResult(this::viewReport)
						.execute();
	}

	private JasperPrint fillCustomerReport() {
		Collection<Long> customerIDs = Entity.values(Customer.ID,
						tableModel().selection().items().get());
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("CUSTOMER_IDS", customerIDs);

		return tableModel().connection()
						.report(Customer.REPORT, reportParameters);
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