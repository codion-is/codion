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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.State;
import is.codion.common.value.Value;

/**
 * A Control for toggling a boolean value.
 */
public interface ToggleControl extends Control {

	/**
	 * @return the value being toggled by this toggle control
	 */
	Value<Boolean> value();

	/**
	 * Returns a {@link Control.Builder} instance, based on a copy of this control, using the given value.
	 * @param value the value for the resulting toggle control
	 * @param <B> the builder type
	 * @return a new builder
	 */
	<B extends Builder<ToggleControl, B>> Builder<ToggleControl, B> copy(Value<Boolean> value);

	/**
	 * Returns a {@link Control.Builder} instance, based on a copy of this control, using the given state.
	 * @param state the state for the resulting toggle control
	 * @param <B> the builder type
	 * @return a new builder
	 */
	<B extends Builder<ToggleControl, B>> Builder<ToggleControl, B> copy(State state);

	/**
	 * Creates a new ToggleControl based on the given value
	 * @param value the value
	 * @return a new ToggleControl
	 */
	static ToggleControl toggleControl(Value<Boolean> value) {
		return builder(value).build();
	}

	/**
	 * Creates a new ToggleControl based on the given state
	 * @param state the state
	 * @return a new ToggleControl
	 */
	static ToggleControl toggleControl(State state) {
		return builder(state).build();
	}

	/**
	 * @param value the value to toggle
	 * @param <B> the builder type
	 * @return a new ToggleControl.Builder
	 */
	static <B extends Builder<ToggleControl, B>> Builder<ToggleControl, B> builder(Value<Boolean> value) {
		return new ToggleControlBuilder<>(value);
	}

	/**
	 * @param state the state to toggle
	 * @param <B> the builder type
	 * @return a new ToggleControl.Builder
	 */
	static <B extends Builder<ToggleControl, B>> Builder<ToggleControl, B> builder(State state) {
		return new ToggleControlBuilder<>(state);
	}
}
