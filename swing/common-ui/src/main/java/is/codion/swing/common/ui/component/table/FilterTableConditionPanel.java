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

import is.codion.common.model.condition.TableConditionModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionState;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static is.codion.swing.common.ui.component.table.FilterTableColumnComponentPanel.filterTableColumnComponentPanel;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A default filter table condition panel.
 * @param <C> the column identifier type
 * @see #filterTableConditionPanel(TableConditionModel, Map, FilterTableColumnModel, Consumer)
 */
public final class FilterTableConditionPanel<C> extends TableConditionPanel<C> {

	private final Map<C, ConditionPanel<?>> conditionPanels;
	private final FilterTableColumnModel<C> columnModel;
	private final Consumer<TableConditionPanel<C>> onPanelInitialized;

	private FilterTableColumnComponentPanel<C> componentPanel;
	private boolean initialized;

	private FilterTableConditionPanel(TableConditionModel<C> tableConditionModel,
																		Map<C, ConditionPanel<?>> conditionPanels,
																		FilterTableColumnModel<C> columnModel,
																		Consumer<TableConditionPanel<C>> onPanelInitialized) {
		super(tableConditionModel, identifier -> Objects.toString(columnModel.column(identifier).getHeaderValue()));
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
	public Map<C, ConditionPanel<?>> get() {
		return conditionPanels;
	}

	@Override
	public Map<C, ConditionPanel<?>> selectable() {
		return conditionPanels.entrySet().stream()
						.filter(entry -> columnModel.visible(entry.getKey()).get())
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * @param <C> the column identifier type
	 * @param tableConditionModel the {@link TableConditionModel}
	 * @param conditionPanels the condition panels
	 * @param columnModel the column model
	 * @param onPanelInitialized called when the panel has been initialized
	 * @return a new {@link FilterTableConditionPanel}
	 */
	public static <C> FilterTableConditionPanel<C> filterTableConditionPanel(TableConditionModel<C> tableConditionModel,
																																					 Map<C, ConditionPanel<?>> conditionPanels,
																																					 FilterTableColumnModel<C> columnModel,
																																					 Consumer<TableConditionPanel<C>> onPanelInitialized) {
		return new FilterTableConditionPanel<>(tableConditionModel, conditionPanels, columnModel, onPanelInitialized);
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
