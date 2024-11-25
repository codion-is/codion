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

import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;
import java.awt.Dimension;

public final class InvoiceLineTablePanel extends EntityTablePanel {

	public InvoiceLineTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						.includeSouthPanel(false)
						.includeConditions(false)
						.includeFilters(false)
						// The invoice should not be editable via the popup menu
						.editable(attributes -> attributes.remove(InvoiceLine.INVOICE_FK))
						// We provide a custom component to use when
						// the track is edited via the popup menu.
						.editComponentFactory(InvoiceLine.TRACK_FK, new TrackComponentFactory(InvoiceLine.TRACK_FK)));
		table().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		setPreferredSize(new Dimension(360, 40));
	}
}
