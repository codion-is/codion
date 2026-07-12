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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.model.condition.TableConditionModel;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.plugin.jasperreports.JasperReports;
import is.codion.swing.common.ui.component.table.ConditionPanel;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.component.table.TableConditionPanel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView.SIMPLE;
import static is.codion.swing.framework.ui.EntityTablePanel.ControlKeys.PRINT;
import static java.util.ResourceBundle.getBundle;

public final class InvoiceTablePanel extends EntityTablePanel {

	private static final ResourceBundle BUNDLE = getBundle(InvoiceTablePanel.class.getName());

	// tag::config[]
	public InvoiceTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						// The TOTAL column is updated automatically when invoice lines are updated,
						// see InvoiceLineEditModel, so we don't want it to be editable via the popup menu.
						.editable(attributes -> attributes.remove(Invoice.TOTAL))
						// The factory providing our custom condition panel.
						.conditionPanel(new InvoiceConditionPanelFactory(tableModel))
						// Start with the SIMPLE condition panel view.
						.conditionView(SIMPLE));
	}
	// end::config[]

	@Override
	protected void setupControls() {
		// Assign a custom report action to the standard PRINT control,
		// which is then made available in the popup menu and on the toolbar
		control(PRINT).set(Control.builder()
						.command(this::viewInvoices)
						.caption(BUNDLE.getString("invoice"))
						.icon(FrameworkIcons.instance().print())
						.enabled(tableModel().selection().empty().not())
						.build());
	}

	private void viewInvoices() {
		Dialogs.progressWorker()
						.task(this::fillInvoices)
						.owner(this)
						.title(BUNDLE.getString("invoice"))
						.onResult(this::viewReport)
						.execute();
	}

	private JasperPrint fillInvoices() {
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("INVOICE_IDS",
						Entity.values(Invoice.ID, tableModel().selection().items().get()));

		return JasperReports.loadPrint(tableModel().connection()
						.report(Invoice.REPORT, reportParameters));
	}

	private void viewReport(JasperPrint invoice) {
		Dialogs.builder()
						.component(new JRViewer(invoice))
						.owner(this)
						.modal(false)
						.title(BUNDLE.getString("invoice"))
						.size(new Dimension(800, 600))
						.show();
	}

	private static final class InvoiceConditionPanelFactory implements TableConditionPanel.Factory<Attribute<?>> {

		private final SwingEntityTableModel tableModel;

		private InvoiceConditionPanelFactory(SwingEntityTableModel tableModel) {
			this.tableModel = tableModel;
		}

		@Override
		public TableConditionPanel<Attribute<?>> create(TableConditionModel<Attribute<?>> tableConditionModel,
																										Map<Attribute<?>, ConditionPanel<?>> conditionPanels,
																										FilterTableColumnModel<Attribute<?>> columnModel,
																										Consumer<TableConditionPanel<Attribute<?>>> onPanelInitialized) {
			return new InvoiceConditionPanel(tableModel, conditionPanels, columnModel, onPanelInitialized);
		}
	}
}
