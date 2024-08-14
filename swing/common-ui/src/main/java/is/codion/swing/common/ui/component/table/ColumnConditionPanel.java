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
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.common.value.Value;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState.*;
import static java.util.Objects.requireNonNull;

/**
 * A base class for a UI component based on a {@link ColumnConditionModel}.
 * @param <C> the type identifying the table columns
 * @param <T> the condition value type
 */
public abstract class ColumnConditionPanel<C, T> extends JPanel {

	private final ColumnConditionModel<C, T> conditionModel;
	private final Value<ConditionState> conditionState = Value.builder()
					.nonNull(HIDDEN)
					.consumer(this::onStateChanged)
					.build();
	private final State hiddenState = State.state(true);
	private final State simpleState = State.state();
	private final State advancedState = State.state();

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

	private final String caption;

	/**
	 * Instantiates a new {@link ColumnConditionPanel} using the column
	 * identifier as caption.
	 * @param conditionModel the condition model
	 */
	protected ColumnConditionPanel(ColumnConditionModel<C, T> conditionModel) {
		this(requireNonNull(conditionModel), conditionModel.identifier().toString());
		configureStates();
	}

	/**
	 * Instantiates a new {@link ColumnConditionPanel}.
	 * @param conditionModel the condition model
	 * @param caption the caption to use when presenting this condition panel
	 */
	protected ColumnConditionPanel(ColumnConditionModel<C, T> conditionModel, String caption) {
		this.conditionModel = requireNonNull(conditionModel);
		this.caption = requireNonNull(caption);
	}

	/**
	 * @return the condition model this panel is based on
	 */
	public final ColumnConditionModel<C, T> conditionModel() {
		return conditionModel;
	}

	/**
	 * @return the value controlling the condition panel state
	 */
	public final Value<ConditionState> state() {
		return conditionState;
	}

	/**
	 * @return the caption to use when presenting this condition panel
	 */
	public final String caption() {
		return caption;
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
	 * The default implementation returns an empty Optional.
	 * @return an event notified when a subcomponent of this condition panel receives focus or an empty Optional if none is available
	 */
	public Optional<EventObserver<C>> focusGainedEvent() {
		return Optional.empty();
	}

	protected abstract void onStateChanged(ConditionState state);

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
