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
import javax.swing.JPanel;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * A base class for a UI component based on a {@link ColumnConditionModel}.
 * @param <C> the type identifying the table columns
 * @param <T> the condition value type
 */
public abstract class ColumnConditionPanel<C, T> extends JPanel {

	private final ColumnConditionModel<C, T> conditionModel;

	/**
	 * The available condition panel states
	 */
	public enum ConditionState {
		/**
		 * The condition panel is hidden.
		 */
		HIDDEN,
		/**
		 * The condition panel is in a simple state.
		 */
		SIMPLE,
		/**
		 * The condition panel is in an advanced state.
		 */
		ADVANCED
	}

	/**
	 * Instantiates a new {@link ColumnConditionPanel}
	 * @param conditionModel the condition model
	 */
	protected ColumnConditionPanel(ColumnConditionModel<C, T> conditionModel) {
		this.conditionModel = requireNonNull(conditionModel);
	}

	/**
	 * @return the condition model this panel is based on
	 */
	public final ColumnConditionModel<C, T> conditionModel() {
		return conditionModel;
	}

	/**
	 * @return the components presented by this condition panel
	 */
	public abstract Collection<JComponent> components();

	/**
	 * Requests keyboard focus for this panel
	 */
	public abstract void requestInputFocus();

	/**
	 * @return the value controlling the condition panel state
	 */
	public abstract Value<ConditionState> state();
}
