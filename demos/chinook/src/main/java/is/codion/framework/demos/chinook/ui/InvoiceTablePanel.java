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
package is.codion.framework.demos.chinook.ui;

import is.codion.common.model.condition.TableConditions;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.component.table.TableConditionsPanel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.util.Collection;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState.SIMPLE;

public final class InvoiceTablePanel extends EntityTablePanel {

	public InvoiceTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						.editable(attributes -> attributes.remove(Invoice.TOTAL))
						.tableConditionsPanelFactory(new InvoiceConditionPanelFactory(tableModel)));
		conditionPanel().state().set(SIMPLE);
	}

	private static final class InvoiceConditionPanelFactory implements TableConditionsPanel.Factory<Attribute<?>> {

		private final SwingEntityTableModel tableModel;

		private InvoiceConditionPanelFactory(SwingEntityTableModel tableModel) {
			this.tableModel = tableModel;
		}

		@Override
		public TableConditionsPanel<Attribute<?>> create(TableConditions<Attribute<?>> conditionModel,
																										 Collection<ColumnConditionPanel<Attribute<?>, ?>> columnConditionPanels,
																										 FilterTableColumnModel<Attribute<?>> columnModel,
																										 Consumer<TableConditionsPanel<Attribute<?>>> onPanelInitialized) {
			return new InvoiceConditionPanel(tableModel.entityDefinition(), conditionModel,
							columnModel, onPanelInitialized, tableModel::refresh);
		}
	}
}
