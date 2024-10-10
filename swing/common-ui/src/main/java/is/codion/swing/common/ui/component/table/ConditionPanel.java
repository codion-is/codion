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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;
import is.codion.common.value.Value;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView.*;
import static java.util.Objects.requireNonNull;

/**
 * An abstract base class for a UI component based on a {@link ConditionModel}.
 * @param <T> the condition value type
 */
public abstract class ConditionPanel<T> extends JPanel {

	private final ConditionModel<T> condition;
	private final Value<ConditionView> conditionView = Value.builder()
					.nonNull(HIDDEN)
					.consumer(this::onViewChanged)
					.build();
	private final State hiddenState = State.state(true);
	private final State simpleState = State.state();
	private final State advancedState = State.state();

	/**
	 * The available condition panel views
	 */
	public enum ConditionView {
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
	 * Instantiates a new {@link ConditionPanel}.
	 * @param condition the condition model
	 */
	protected ConditionPanel(ConditionModel<T> condition) {
		this.condition = requireNonNull(condition);
		configureStates();
	}

	/**
	 * @return the condition model this panel is based on
	 */
	public final ConditionModel<T> condition() {
		return condition;
	}

	/**
	 * @return the {@link Value} controlling the condition panel view
	 */
	public final Value<ConditionView> conditionView() {
		return conditionView;
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
	 * @return an observer notified when a subcomponent of this condition panel receives focus or an empty Optional if none is available
	 */
	public Optional<Observer<?>> focusGainedObserver() {
		return Optional.empty();
	}

	protected abstract void onViewChanged(ConditionView conditionView);

	private void configureStates() {
		State.group(hiddenState, simpleState, advancedState);
		hiddenState.addConsumer(new ViewConsumer(HIDDEN));
		simpleState.addConsumer(new ViewConsumer(SIMPLE));
		advancedState.addConsumer(new ViewConsumer(ADVANCED));
		conditionView.addConsumer(view -> {
			hiddenState.set(view == HIDDEN);
			simpleState.set(view == SIMPLE);
			advancedState.set(view == ADVANCED);
		});
	}

	private final class ViewConsumer implements Consumer<Boolean> {

		private final ConditionView conditionView;

		private ViewConsumer(ConditionView conditionView) {
			this.conditionView = conditionView;
		}

		@Override
		public void accept(Boolean enabled) {
			if (enabled) {
				ConditionPanel.this.conditionView.set(conditionView);
			}
		}
	}
}