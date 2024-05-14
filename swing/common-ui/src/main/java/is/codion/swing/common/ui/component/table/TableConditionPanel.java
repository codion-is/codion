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
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * A base class for a UI component based on a {@link TableConditionModel}.
 * @param <C> the type used to identify the table columns
 */
public abstract class TableConditionPanel<C> extends JPanel {

	private final TableConditionModel<C> conditionModel;

	/**
	 * Instantiates a new {@link TableConditionPanel}
	 * @param conditionModel the condition model
	 */
	protected TableConditionPanel(TableConditionModel<C> conditionModel) {
		this.conditionModel = requireNonNull(conditionModel);
	}

	/**
	 * @return the underlying condition model
	 */
	public final TableConditionModel<C> conditionModel() {
		return conditionModel;
	}

	/**
	 * @return an unmodifiable view of the condition panels
	 */
	public abstract Collection<ColumnConditionPanel<C, ?>> conditionPanels();

	/**
	 * @return the value controlling the condition panel state
	 */
	public abstract Value<ConditionState> state();

	/**
	 * @param <T> the column value type
	 * @param columnIdentifier the column identifier
	 * @return the condition panel associated with the given column
	 * @throws IllegalStateException in case no panel is available
	 */
	public abstract <T extends ColumnConditionPanel<C, ?>> T conditionPanel(C columnIdentifier);

	/**
	 * @return the controls provided by this condition panel, for example toggling the advanced mode and clearing the condition
	 */
	public abstract Controls controls();

	/**
	 * Selects one conditon panel to receive the input focus.
	 * If only one panel is available, that one receives the input focus automatically.
	 * If multiple conditon panels are available a selection dialog is presented.
	 * Override to implement.
	 * @param dialogOwner the dialog owner
	 */
	public void selectCondition(JComponent dialogOwner) {}

	/**
	 * @param <C> the type identifying the table columns
	 */
	public interface Factory<C> {

		/**
		 * @param conditionModel the condition model
		 * @param conditionPanels the condition panels
		 * @param columnModel the column model
		 * @return a new {@link TableConditionPanel}
		 */
		TableConditionPanel<C> create(TableConditionModel<C> conditionModel,
																	Collection<ColumnConditionPanel<C, ?>> conditionPanels,
																	FilterTableColumnModel<C> columnModel);
	}
}
