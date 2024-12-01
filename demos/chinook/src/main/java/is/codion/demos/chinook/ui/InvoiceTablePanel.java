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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.model.condition.TableConditionModel;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.ConditionPanel;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.component.table.TableConditionPanel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.util.Map;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView.SIMPLE;

public final class InvoiceTablePanel extends EntityTablePanel {

	public InvoiceTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						// The TOTAL column is updated automatically when invoice lines are updated,
						// see InvoiceLineEditModel, so we don't want it to be editable via the popup menu.
						.editable(attributes -> attributes.remove(Invoice.TOTAL))
						// The factory providing our custom condition panel.
						.conditionPanelFactory(new InvoiceConditionPanelFactory(tableModel))
						// Start with the SIMPLE condition panel view.
						.conditionView(SIMPLE));
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
