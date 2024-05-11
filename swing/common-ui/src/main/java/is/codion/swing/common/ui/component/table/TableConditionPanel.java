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

import is.codion.common.model.table.TableConditionModel;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.control.Controls;

import java.util.Collection;
import java.util.Optional;

/**
 * A UI component based on a {@link TableConditionModel}.
 * @param <C> the type used to identify the table columns
 */
public interface TableConditionPanel<C> {

	/**
	 * @return the underlying condition model
	 */
	TableConditionModel<C> conditionModel();

	/**
	 * @return an unmodifiable view of the condition panels
	 */
	Collection<? extends ColumnConditionPanel<C, ?>> conditionPanels();

	/**
	 * @return the value controlling the condition panel state
	 */
	Value<ColumnConditionPanel.ConditionState> state();

	/**
	 * @param <T> the column value type
	 * @param columnIdentifier the column identifier
	 * @return the condition panel associated with the given column or an empty Optional in case none exists
	 */
	<T extends ColumnConditionPanel<C, ?>> Optional<T> conditionPanel(C columnIdentifier);

	/**
	 * @return the controls provided by this condition panel, for example toggling the advanced mode and clearing the condition
	 */
	Controls controls();
}
