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

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.value.Value;

import javax.swing.JComponent;
import java.util.Collection;

/**
 * A UI component for a {@link ColumnConditionModel}.
 * @param <C> the type identifying the table columns
 * @param <T> the condition value type
 */
public interface ColumnConditionPanel<C, T> {

	/**
	 * The available condition panel states
	 */
	enum ConditionState {
		HIDDEN,
		SIMPLE,
		ADVANCED
	}

	/**
	 * @return the condition model this panel is based on
	 */
	ColumnConditionModel<C, T> conditionModel();

	/**
	 * @return the components presented by this condition panel
	 */
	Collection<JComponent> components();

	/**
	 * Requests keyboard focus for this panel
	 */
	void requestInputFocus();

	/**
	 * @return the value controlling the condition panel state
	 */
	Value<ConditionState> state();
}
