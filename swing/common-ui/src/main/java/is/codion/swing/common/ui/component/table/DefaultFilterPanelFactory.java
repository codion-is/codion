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
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.condition.TableConditions;

import java.util.Collection;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.table.FilterTableConditionsPanel.filterTableConditionsPanel;

final class DefaultFilterPanelFactory<C> implements TableConditionsPanel.Factory<C> {

	@Override
	public TableConditionsPanel<C> create(TableConditions<C> conditionModel,
																				Collection<ColumnConditionPanel<C, ?>> columnConditionPanels,
																				FilterTableColumnModel<C> columnModel,
																				Consumer<TableConditionsPanel<C>> onPanelInitialized) {
		return filterTableConditionsPanel(conditionModel, columnConditionPanels, columnModel, onPanelInitialized);
	}
}
