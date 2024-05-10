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

import is.codion.common.event.EventObserver;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.state.State;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.JPanel;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A UI component presenting one or more {@link AbstractColumnConditionPanel}s.
 * @param <C> the type used to identify the table columns
 */
public abstract class AbstractFilterTableConditionPanel<C> extends JPanel {

	private final TableConditionModel<C> conditionModel;
	private final List<? extends AbstractColumnConditionPanel<C, ?>> conditionPanels;

	/**
	 * @param conditionModel the condition model
	 * @param conditionPanels the condition panels
	 */
	protected AbstractFilterTableConditionPanel(TableConditionModel<? extends C> conditionModel,
																							List<? extends AbstractColumnConditionPanel<? extends C, ?>> conditionPanels) {
		this.conditionModel = (TableConditionModel<C>) requireNonNull(conditionModel);
		this.conditionPanels = (List<? extends AbstractColumnConditionPanel<C, ?>>) requireNonNull(conditionPanels);
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
	public final Collection<? extends AbstractColumnConditionPanel<C, ?>> conditionPanels() {
		return conditionPanels;
	}

	/**
	 * @return the state controlling the advanced view status of this condition panel
	 */
	public abstract State advanced();

	/**
	 * @param <T> the column value type
	 * @param columnIdentifier the column identifier
	 * @return the condition panel associated with the given column
	 * @throws IllegalArgumentException in case the column has no condition panel
	 */
	public final <T extends AbstractColumnConditionPanel<C, ?>> T conditionPanel(C columnIdentifier) {
		return (T) conditionPanels.stream()
						.filter(conditionPanel -> conditionPanel.conditionModel().columnIdentifier().equals(columnIdentifier))
						.findFirst()
						.orElseThrow(() -> new IllegalArgumentException("No condition panel found for column identifier " + columnIdentifier));
	}

	/**
	 * @return the controls provided by this condition panel, for example toggling the advanced mode and clearing the condition
	 */
	public abstract Controls controls();

	/**
	 * @return an observer notified when a condition panel receives focus
	 */
	public abstract EventObserver<C> focusGainedEvent();
}
