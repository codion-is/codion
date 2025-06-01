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
package is.codion.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;

import java.util.Collection;

import static javax.swing.SwingUtilities.invokeLater;

public final class InvoiceModel extends SwingEntityModel {

	public InvoiceModel(EntityConnectionProvider connectionProvider) {
		super(new InvoiceEditModel(connectionProvider));

		InvoiceLineEditModel invoiceLineEditModel = new InvoiceLineEditModel(connectionProvider);

		SwingEntityModel invoiceLineModel = new SwingEntityModel(invoiceLineEditModel);
		detailModels().add(link(invoiceLineModel)
						// Prevents accidentally adding a new invoice line to the previously selected invoice,
						// since the selected foreign key value persists when the master selection is cleared by default.
						.clearValueOnEmptySelection(true)
						// Usually the UI is responsible for activating the detail model link for the currently
						// active (or visible) detail panel, but since the InvoiceLine panel is embedded in the
						// InvoiceEditPanel, we simply activate the link here.
						.active(true)
						.build());

		// We listen for when invoice totals are updated by the edit model,
		// and replace the invoices in the table model with the updated ones.
		// Note the use of invokeLater() since the event is triggered during
		// update, which happens in a background thread, so we have to update
		// the table data on the Event Dispatch Thread.
		invoiceLineEditModel.totalsUpdated().addConsumer(this::onTotalsUpdated);
	}

	private void onTotalsUpdated(Collection<Entity> updatedInvoices) {
		invokeLater(() -> tableModel().replace(updatedInvoices));
	}
}
