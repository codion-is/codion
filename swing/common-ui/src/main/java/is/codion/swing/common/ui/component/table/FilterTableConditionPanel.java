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
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static is.codion.swing.common.ui.component.table.FilterTableColumnComponentPanel.filterTableColumnComponentPanel;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * A default filter table condition panel.
 * @param <C> the column identifier type
 * @see #filterTableConditionPanel(TableConditionModel, Collection, FilterTableColumnModel, Consumer)
 */
public final class FilterTableConditionPanel<C> extends TableConditionPanel<C> {

	private final Collection<ColumnConditionPanel<C, ?>> conditionPanels;
	private final FilterTableColumnModel<C> columnModel;
	private final Consumer<TableConditionPanel<C>> onPanelInitialized;

	private FilterTableColumnComponentPanel<C> componentPanel;
	private boolean initialized;

	private FilterTableConditionPanel(TableConditionModel<C> conditionModel,
																		Collection<ColumnConditionPanel<C, ?>> conditionPanels,
																		FilterTableColumnModel<C> columnModel,
																		Consumer<TableConditionPanel<C>> onPanelInitialized) {
		super(conditionModel);
		this.conditionPanels = unmodifiableList(new ArrayList<>(requireNonNull(conditionPanels)));
		this.columnModel = requireNonNull(columnModel);
		this.onPanelInitialized = onPanelInitialized == null ? panel -> {} : onPanelInitialized;
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(componentPanel);
	}

	@Override
	public Collection<ColumnConditionPanel<C, ?>> conditionPanels() {
		return conditionPanels;
	}

	@Override
	public Collection<ColumnConditionPanel<C, ?>> selectableConditionPanels() {
		return conditionPanels.stream()
						.filter(conditionPanel -> columnModel.visible(conditionPanel.condition().identifier()).get())
						.collect(Collectors.toList());
	}

	/**
	 * @param <C> the column identifier type
	 * @param conditionModel the condition model
	 * @param conditionPanels the condition panels
	 * @param columnModel the column model
	 * @param onPanelInitialized called when the panel has been initialized
	 * @return a new {@link FilterTableConditionPanel}
	 */
	public static <C> FilterTableConditionPanel<C> filterTableConditionPanel(TableConditionModel<C> conditionModel,
																																					 Collection<ColumnConditionPanel<C, ?>> conditionPanels,
																																					 FilterTableColumnModel<C> columnModel,
																																					 Consumer<TableConditionPanel<C>> onPanelInitialized) {
		return new FilterTableConditionPanel<>(conditionModel, conditionPanels, columnModel, onPanelInitialized);
	}

	@Override
	protected void onStateChanged(ConditionState conditionState) {
		conditionPanels.forEach(panel -> panel.state().set(conditionState));
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
			Map<C, ColumnConditionPanel<C, ?>> conditionPanelMap = conditionPanels.stream()
							.collect(toMap(panel -> panel.condition().identifier(), identity()));
			componentPanel = filterTableColumnComponentPanel(columnModel, conditionPanelMap);
			onPanelInitialized.accept(this);
			initialized = true;
		}
	}
}
