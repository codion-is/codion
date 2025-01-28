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
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

public final class InvoicePanel extends EntityPanel {

	public InvoicePanel(SwingEntityModel invoiceModel) {
		super(invoiceModel,
						new InvoiceEditPanel(invoiceModel.editModel(),
										invoiceModel.detailModels().get(InvoiceLine.TYPE)),
						new InvoiceTablePanel(invoiceModel.tableModel()),
						// The InvoiceLine panel is embedded in InvoiceEditPanel,
						// so this panel doesn't need a detail panel layout.
						config -> config.detailLayout(DetailLayout.NONE));
		InvoiceEditPanel editPanel = (InvoiceEditPanel) editPanel();
		// We still add the InvoiceLine panel as a detail panel for keyboard navigation
		detailPanels().add(editPanel.invoiceLinePanel());
	}
}
