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

import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

public final class InvoicePanel extends EntityPanel {

	public InvoicePanel(SwingEntityModel invoiceModel, EntityPanel invoiceLinePanel) {
		super(invoiceModel, new InvoiceEditPanel(invoiceModel.editModel(), invoiceLinePanel),
						new InvoiceTablePanel(invoiceModel.tableModel()),
						config -> config.detailLayout(DetailLayout.NONE));
		addDetailPanel(invoiceLinePanel);
	}
}
