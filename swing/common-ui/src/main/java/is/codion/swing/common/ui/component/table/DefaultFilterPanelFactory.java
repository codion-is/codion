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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.condition.TableConditionModel;

import java.util.Map;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.table.FilterTableConditionPanel.filterTableConditionPanel;

final class DefaultFilterPanelFactory<C> implements TableConditionPanel.Factory<C> {

	@Override
	public TableConditionPanel<C> create(TableConditionModel<C> tableConditionModel,
																			 Map<C, ConditionPanel<?>> conditionPanels,
																			 FilterTableColumnModel<C> columnModel,
																			 Consumer<TableConditionPanel<C>> onPanelInitialized) {
		return filterTableConditionPanel(tableConditionModel, conditionPanels, columnModel, onPanelInitialized);
	}
}
