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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.State;
import is.codion.common.value.Value;

import javax.swing.KeyStroke;

import static java.util.Objects.requireNonNull;

/**
 * A Control for toggling a boolean value.
 */
public interface ToggleControl extends Control {

	/**
	 * @return the value being toggled by this toggle control
	 */
	Value<Boolean> value();

	/**
	 * Returns a {@link ToggleControlBuilder} instance, based on a copy of this control, using the same value
	 * @return a new builder
	 */
	@Override
	ToggleControlBuilder copy();

	/**
	 * Returns a new {@link ToggleControlBuilder} instance, based on a copy of this control, using the given value.
	 * @param value the value for the resulting toggle control
	 * @return a new builder
	 */
	ToggleControlBuilder copy(Value<Boolean> value);

	/**
	 * Returns a new {@link ToggleControlBuilder} instance, based on a copy of this control, using the given state.
	 * @param state the state for the resulting toggle control
	 * @return a new builder
	 */
	ToggleControlBuilder copy(State state);

	/**
	 * @param name the control name
	 * @return a new {@link ControlKey} for identifying a {@link ToggleControl} instance
	 */
	static ControlKey<ToggleControl> key(String name) {
		return new DefaultControlKey<>(name, ToggleControl.class, null);
	}

	/**
	 * @param name the control name
	 * @param defaultKeyStroke the default keystroke
	 * @return a new {@link ControlKey} for identifying a {@link ToggleControl} instance
	 */
	static ControlKey<ToggleControl> key(String name, KeyStroke defaultKeyStroke) {
		return new DefaultControlKey<>(name, ToggleControl.class, requireNonNull(defaultKeyStroke));
	}

	/**
	 * Builds a {@link ToggleControl}
	 */
	interface ToggleControlBuilder extends Builder<ToggleControl, ToggleControlBuilder> {}
}
