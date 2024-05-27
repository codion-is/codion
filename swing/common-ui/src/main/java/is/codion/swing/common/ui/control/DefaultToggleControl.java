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

import is.codion.common.event.Event;
import is.codion.common.state.State;
import is.codion.common.value.Value;

import java.awt.event.ActionEvent;

import static java.util.Objects.requireNonNull;

/**
 * A default ToggleControl implementation.
 */
final class DefaultToggleControl extends AbstractControl implements ToggleControl {

	private final Value<Boolean> value;

	/**
	 * @param value the value to toggle
	 * @param name the name
	 * @param enabled an observer indicating when this control should be enabled
	 */
	DefaultToggleControl(DefaultToggleControlBuilder builder) {
		super(builder);
		this.value = requireNonNull(builder.value, "value");
	}

	@Override
	public Value<Boolean> value() {
		return value;
	}

	@Override
	public void actionPerformed(ActionEvent e) {/*Not required*/}

	@Override
	public ToggleControlBuilder copy() {
		return copy(value);
	}

	@Override
	public ToggleControlBuilder copy(Value<Boolean> value) {
		ToggleControlBuilder builder = new DefaultToggleControlBuilder(value)
						.enabled(enabled());
		keys().forEach(key -> builder.value(key, getValue(key)));

		return builder;
	}

	@Override
	public ToggleControlBuilder copy(State state) {
		return copy((Value<Boolean>) state);
	}

	@Override
	public CommandControlBuilder copy(Command command) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CommandControlBuilder copy(ActionCommand actionCommand) {
		throw new UnsupportedOperationException();
	}
}
