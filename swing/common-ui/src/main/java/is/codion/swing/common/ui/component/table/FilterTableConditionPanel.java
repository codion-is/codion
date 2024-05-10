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

import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.state.State;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Function;

import static is.codion.swing.common.ui.component.table.FilterTableColumnComponentPanel.filterTableColumnComponentPanel;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

/**
 * A default filter table condition panel.
 * @param <C> the column identifier type
 * @see #filterTableConditionPanel(TableConditionModel, List, FilterTableColumnModel)
 */
public final class FilterTableConditionPanel<C> extends AbstractFilterTableConditionPanel<C> {

	private final FilterTableColumnComponentPanel<C, AbstractColumnConditionPanel<? extends C, ?>> componentPanel;
	private final State advanced = State.builder()
					.consumer(this::onAdvancedChanged)
					.build();
	private final Event<C> focusGainedEvent = Event.event();

	private FilterTableConditionPanel(TableConditionModel<? extends C> tableConditionModel,
																		List<AbstractColumnConditionPanel<? extends C, ?>> conditionPanels,
																		FilterTableColumnModel<C> columnModel) {
		super(tableConditionModel, conditionPanels);
		this.componentPanel = filterTableColumnComponentPanel(requireNonNull(columnModel), conditionPanels.stream()
						.collect(toMap(conditionPanel -> conditionPanel.conditionModel().columnIdentifier(), Function.identity())));
		setLayout(new BorderLayout());
		add(componentPanel, BorderLayout.CENTER);
		componentPanel.components().values().forEach(panel ->
						panel.focusGainedEvent().addConsumer(focusGainedEvent));
	}

	@Override
	public State advanced() {
		return advanced;
	}

	@Override
	public Controls controls() {
		return Controls.builder()
						.control(ToggleControl.builder(advanced)
										.name(Messages.advanced()))
						.control(Control.builder(this::clearConditions)
										.name(Messages.clear()))
						.build();
	}

	@Override
	public EventObserver<C> focusGainedEvent() {
		return focusGainedEvent.observer();
	}

	/**
	 * @param <C> the column identifier type
	 * @param conditionModel the condition model
	 * @param conditionPanels the condition panels
	 * @param columnModel the column model
	 * @return a new {@link FilterTableConditionPanel}
	 */
	public static <C> FilterTableConditionPanel<? extends C> filterTableConditionPanel(TableConditionModel<C> conditionModel,
																																										 List<AbstractColumnConditionPanel<? extends C, ?>> conditionPanels,
																																										 FilterTableColumnModel<C> columnModel) {
		return new FilterTableConditionPanel<>(conditionModel, conditionPanels, columnModel);
	}

	private void clearConditions() {
		componentPanel.components().values().stream()
						.map(AbstractColumnConditionPanel::conditionModel)
						.forEach(ColumnConditionModel::clear);
	}

	private void onAdvancedChanged(boolean advancedView) {
		componentPanel.components().forEach((column, panel) -> panel.advanced().set(advancedView));
	}
}
