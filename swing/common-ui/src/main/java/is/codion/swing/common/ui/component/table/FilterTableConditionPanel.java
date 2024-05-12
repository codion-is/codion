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

import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.component.table.FilterTableColumnComponentPanel.filterTableColumnComponentPanel;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toMap;

/**
 * A default filter table condition panel.
 * @param <C> the column identifier type
 * @see #filterTableConditionPanel(TableConditionModel, Collection, FilterTableColumnModel)
 */
public final class FilterTableConditionPanel<C> extends JPanel implements TableConditionPanel<C> {

	private static final MessageBundle MESSAGES =
					messageBundle(FilterColumnConditionPanel.class, getBundle(FilterTableConditionPanel.class.getName()));

	private final TableConditionModel<C> conditionModel;
	private final Collection<ColumnConditionPanel<C, ?>> conditionPanels;
	private final FilterTableColumnComponentPanel<C> componentPanel;
	private final Value<ConditionState> conditionState = Value.nonNull(ConditionState.HIDDEN)
					.consumer(this::onStateChanged)
					.build();
	private final State hiddenState = State.state(true);
	private final State simpleState = State.state();
	private final State advancedState = State.state();

	private FilterTableConditionPanel(TableConditionModel<C> conditionModel,
																		Collection<ColumnConditionPanel<C, ?>> conditionPanels,
																		FilterTableColumnModel<C> columnModel) {
		this.conditionModel = requireNonNull(conditionModel);
		this.conditionPanels = unmodifiableList(new ArrayList<>(requireNonNull(conditionPanels)));
		Map<C, JComponent> collect = conditionPanels.stream()
						.collect(toMap(panel -> panel.conditionModel()
										.columnIdentifier(), JComponent.class::cast));
		this.componentPanel = filterTableColumnComponentPanel(requireNonNull(columnModel), collect);
		setLayout(new BorderLayout());
		configureStates();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(componentPanel);
	}

	@Override
	public TableConditionModel<C> conditionModel() {
		return conditionModel;
	}

	@Override
	public Collection<ColumnConditionPanel<C, ?>> conditionPanels() {
		return conditionPanels;
	}

	@Override
	public <T extends ColumnConditionPanel<C, ?>> Optional<T> conditionPanel(C columnIdentifier) {
		return (Optional<T>) conditionPanels.stream()
						.filter(panel -> panel.conditionModel().columnIdentifier().equals(columnIdentifier))
						.findFirst();
	}

	@Override
	public Value<ConditionState> state() {
		return conditionState;
	}

	@Override
	public Controls controls() {
		return Controls.builder()
						.control(ToggleControl.builder(hiddenState)
										.name(MESSAGES.getString("hidden")))
						.control(ToggleControl.builder(simpleState)
										.name(MESSAGES.getString("simple")))
						.control(ToggleControl.builder(advancedState)
										.name(MESSAGES.getString("advanced")))
						.separator()
						.control(Control.builder(this::clearConditions)
										.name(Messages.clear()))
						.build();
	}

	/**
	 * @param <C> the column identifier type
	 * @param conditionModel the condition model
	 * @param conditionPanels the condition panels
	 * @param columnModel the column model
	 * @return a new {@link FilterTableConditionPanel}
	 */
	public static <C> FilterTableConditionPanel<C> filterTableConditionPanel(TableConditionModel<C> conditionModel,
																																										 Collection<ColumnConditionPanel<C, ?>> conditionPanels,
																																										 FilterTableColumnModel<C> columnModel) {
		return new FilterTableConditionPanel<>(conditionModel, conditionPanels, columnModel);
	}

	private void clearConditions() {
		conditionPanels.stream()
						.map(ColumnConditionPanel::conditionModel)
						.forEach(ColumnConditionModel::clear);
	}

	private void onStateChanged(ConditionState conditionState) {
		conditionPanels.forEach(panel -> panel.state().set(conditionState));
		switch (conditionState) {
			case HIDDEN:
				remove(componentPanel);
				break;
			case SIMPLE:
			case ADVANCED:
				add(componentPanel, BorderLayout.CENTER);
				break;
			default:
				throw new IllegalArgumentException("Unknown panel state: " + conditionState);
		}
		revalidate();
	}

	private void configureStates() {
		State.group(hiddenState, simpleState, advancedState);
		hiddenState.addConsumer(new StateConsumer(ConditionState.HIDDEN));
		simpleState.addConsumer(new StateConsumer(ConditionState.SIMPLE));
		advancedState.addConsumer(new StateConsumer(ConditionState.ADVANCED));
		conditionState.addConsumer(state -> {
			hiddenState.set(state == ConditionState.HIDDEN);
			simpleState.set(state == ConditionState.SIMPLE);
			advancedState.set(state == ConditionState.ADVANCED);
		});
	}

	private final class StateConsumer implements Consumer<Boolean> {
		private final ConditionState state;

		private StateConsumer(ConditionState state) {
			this.state = state;
		}

		@Override
		public void accept(Boolean enabled) {
			if (enabled) {
				conditionState.set(state);
			}
		}
	}
}
