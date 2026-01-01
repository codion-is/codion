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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.demos.chinook.model.CustomerModel;
import is.codion.swing.framework.ui.EntityPanel;

public final class CustomerPanel extends EntityPanel {

	public CustomerPanel(CustomerModel customerModel) {
		super(customerModel,
						new CustomerEditPanel(customerModel.editModel()),
						new CustomerTablePanel(customerModel.tableModel()));

		detailPanels().add(new InvoicePanel(customerModel.detailModels().get(Invoice.TYPE)));
	}
}
