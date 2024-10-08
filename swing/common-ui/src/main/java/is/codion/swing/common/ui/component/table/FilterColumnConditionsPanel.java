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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.condition.ColumnConditions;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.table.FilterTableColumnComponentPanel.filterTableColumnComponentPanel;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

/**
 * A default filter table condition panel.
 * @param <C> the column identifier type
 * @see #filterColumnConditionsPanel(ColumnConditions, Collection, FilterTableColumnModel, Consumer)
 */
public final class FilterColumnConditionsPanel<C> extends ColumnConditionsPanel<C> {

	private final Map<C, ColumnConditionPanel<?>> conditionPanels;
	private final FilterTableColumnModel<C> columnModel;
	private final Consumer<ColumnConditionsPanel<C>> onPanelInitialized;

	private FilterTableColumnComponentPanel<C> componentPanel;
	private boolean initialized;

	private FilterColumnConditionsPanel(ColumnConditions<C> columnConditions,
																			Map<C, ColumnConditionPanel<?>> conditionPanels,
																			FilterTableColumnModel<C> columnModel,
																			Consumer<ColumnConditionsPanel<C>> onPanelInitialized) {
		super(columnConditions);
		this.conditionPanels = unmodifiableMap(new HashMap<>(requireNonNull(conditionPanels)));
		this.columnModel = requireNonNull(columnModel);
		this.onPanelInitialized = onPanelInitialized == null ? panel -> {} : onPanelInitialized;
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(componentPanel);
	}

	@Override
	public Map<C, ColumnConditionPanel<?>> conditionPanels() {
		return conditionPanels;
	}

	@Override
	public Map<C, ColumnConditionPanel<?>> selectableConditionPanels() {
		return conditionPanels.entrySet().stream()
						.filter(entry -> columnModel.visible(entry.getKey()).get())
						.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * @param <C> the column identifier type
	 * @param columnConditions the {@link ColumnConditions}
	 * @param conditionPanels the condition panels
	 * @param columnModel the column model
	 * @param onPanelInitialized called when the panel has been initialized
	 * @return a new {@link FilterColumnConditionsPanel}
	 */
	public static <C> FilterColumnConditionsPanel<C> filterColumnConditionsPanel(ColumnConditions<C> columnConditions,
																																							 Map<C, ColumnConditionPanel<?>> conditionPanels,
																																							 FilterTableColumnModel<C> columnModel,
																																							 Consumer<ColumnConditionsPanel<C>> onPanelInitialized) {
		return new FilterColumnConditionsPanel<>(columnConditions, conditionPanels, columnModel, onPanelInitialized);
	}

	@Override
	protected void onStateChanged(ConditionState conditionState) {
		conditionPanels.values().forEach(panel -> panel.state().set(conditionState));
		switch (conditionState) {
			case HIDDEN:
				remove(componentPanel);
				break;
			case SIMPLE:
			case ADVANCED:
				initialize();
				add(componentPanel, BorderLayout.CENTER);
				break;
			default:
				throw new IllegalArgumentException("Unknown panel state: " + conditionState);
		}
		revalidate();
	}

	private void initialize() {
		if (!initialized) {
			setLayout(new BorderLayout());
			componentPanel = filterTableColumnComponentPanel(columnModel, conditionPanels);
			onPanelInitialized.accept(this);
			initialized = true;
		}
	}
}
