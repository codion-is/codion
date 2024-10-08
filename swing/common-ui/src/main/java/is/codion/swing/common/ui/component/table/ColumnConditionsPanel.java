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

import is.codion.common.Text;
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.model.condition.ColumnConditions;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionState;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static is.codion.common.item.Item.item;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.component.table.ConditionPanel.ConditionState.*;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;

/**
 * A base class for a UI component based on a {@link ColumnConditions}.
 * @param <C> the type used to identify the table columns
 */
public abstract class ColumnConditionsPanel<C> extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(FilterColumnConditionPanel.class, getBundle(ColumnConditionsPanel.class.getName()));

	private final ColumnConditions<C> columnConditions;
	private final Function<C, String> captions;
	private final Value<ConditionState> conditionState = Value.builder()
					.nonNull(HIDDEN)
					.consumer(this::onStateChanged)
					.build();
	private final State hiddenState = State.state(true);
	private final State simpleState = State.state();
	private final State advancedState = State.state();

	/**
	 * Instantiates a new {@link ColumnConditionsPanel}
	 * @param columnConditions the {@link ColumnConditions}
	 * @param captions provides captions based on the column identifiers presenting condition selection
	 * @see #select(JComponent)
	 */
	protected ColumnConditionsPanel(ColumnConditions<C> columnConditions, Function<C, String> captions) {
		this.columnConditions = requireNonNull(columnConditions);
		this.captions = requireNonNull(captions);
		configureStates();
	}

	/**
	 * @return the underlying {@link ColumnConditions}
	 */
	public final ColumnConditions<C> conditions() {
		return columnConditions;
	}

	/**
	 * @return the {@link Value} controlling the condition panel state
	 */
	public final Value<ConditionState> state() {
		return conditionState;
	}

	/**
	 * @return an unmodifiable view of the condition panels
	 */
	public abstract Map<C, ConditionPanel<?>> panels();

	/**
	 * By default this returns all condition panels, override to customize.
	 * @return the selectable condition panels
	 * @see #select(JComponent)
	 */
	public Map<C, ConditionPanel<?>> selectable() {
		return panels();
	}

	/**
	 * @param <T> the column value type
	 * @param identifier the column identifier
	 * @return the condition panel associated with the given column
	 * @throws IllegalStateException in case no panel is available
	 */
	public <T extends ConditionPanel<?>> T panel(C identifier) {
		requireNonNull(identifier);
		ConditionPanel<?> conditionPanel = panels().get(identifier);
		if (conditionPanel == null) {
			throw new IllegalStateException("No condition panel available for " + identifier);
		}

		return (T) conditionPanel;
	}

	/**
	 * @return the controls provided by this condition panel, for example clearing the condition and toggling the condition state
	 */
	public Controls controls() {
		return Controls.builder()
						.control(Control.builder()
										.toggle(hiddenState)
										.name(MESSAGES.getString("hidden")))
						.control(Control.builder()
										.toggle(simpleState)
										.name(MESSAGES.getString("simple")))
						.control(Control.builder()
										.toggle(advancedState)
										.name(MESSAGES.getString("advanced")))
						.separator()
						.control(Control.builder()
										.command(this::clearConditions)
										.name(Messages.clear()))
						.build();
	}

	/**
	 * Selects one condition panel to receive the input focus.
	 * If only one panel is available, that one receives the input focus automatically.
	 * If multiple condition panels are available a selection dialog is presented.
	 * @param dialogOwner the selection dialog owner
	 */
	public final void select(JComponent dialogOwner) {
		List<Item<? extends ConditionPanel<?>>> panelItems = selectable().entrySet().stream()
						.map(entry -> item(entry.getValue(), captions.apply(entry.getKey())))
						.sorted(Text.collator())
						.collect(toList());
		if (panelItems.size() == 1) {
			state().map(state -> state == HIDDEN ? SIMPLE : state);
			panelItems.get(0).value().requestInputFocus();
		}
		else if (!panelItems.isEmpty()) {
			Dialogs.selectionDialog(panelItems)
							.owner(dialogOwner)
							.title(MESSAGES.getString("select_condition"))
							.selectSingle()
							.map(Item::value)
							.ifPresent(conditionPanel -> {
								state().map(state -> state == HIDDEN ? SIMPLE : state);
								conditionPanel.requestInputFocus();
							});
		}
	}

	/**
	 * Called each time the condition state changes, override to update this panel according to the state
	 * @param conditionState the new condition state
	 */
	protected void onStateChanged(ConditionState conditionState) {}

	private void configureStates() {
		State.group(hiddenState, simpleState, advancedState);
		hiddenState.addConsumer(new StateConsumer(HIDDEN));
		simpleState.addConsumer(new StateConsumer(SIMPLE));
		advancedState.addConsumer(new StateConsumer(ADVANCED));
		conditionState.addConsumer(state -> {
			hiddenState.set(state == HIDDEN);
			simpleState.set(state == SIMPLE);
			advancedState.set(state == ADVANCED);
		});
	}

	private void clearConditions() {
		columnConditions.get().values()
						.forEach(ConditionModel::clear);
	}

	/**
	 * @param <C> the type identifying the table columns
	 */
	public interface Factory<C> {

		/**
		 * @param conditionModel the condition model
		 * @param conditionPanels the condition panels
		 * @param columnModel the column model
		 * @param onPanelInitialized called when the panel has been initialized
		 * @return a new {@link ColumnConditionsPanel}
		 */
		ColumnConditionsPanel<C> create(ColumnConditions<C> conditionModel,
																		Map<C, ConditionPanel<?>> conditionPanels,
																		FilterTableColumnModel<C> columnModel,
																		Consumer<ColumnConditionsPanel<C>> onPanelInitialized);
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
