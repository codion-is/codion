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
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

public final class InvoicePanel extends EntityPanel {

	public InvoicePanel(SwingEntityModel invoiceModel) {
		super(invoiceModel,
						new InvoiceEditPanel(invoiceModel.editModel(), invoiceModel.detailModel(InvoiceLine.TYPE)),
						new InvoiceTablePanel(invoiceModel.tableModel()),
						// The InvoiceLine panel is embedded in InvoiceEditPanel,
						// so this panel doesn't need to lay out any detail panels.
						config -> config.detailLayout(DetailLayout.NONE));
		InvoiceEditPanel editPanel = editPanel();
		// We still add the InvoiceLine panel as a detail panel for keyboard navigation
		addDetailPanel(editPanel.invoiceLinePanel());
	}
}
