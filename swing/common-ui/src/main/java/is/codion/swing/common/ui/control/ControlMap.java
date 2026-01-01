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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.reactive.value.Value;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.KeyStroke;
import java.util.Collection;
import java.util.Optional;

/**
 * Manages a set of {@link Control} instances.
 */
public interface ControlMap {

	/**
	 * @param controlKey the control key
	 * @param <T> the control type
	 * @return the {@link Value} specifying the {@link Control} associated with the given key
	 * @throws IllegalArgumentException in case no control is associated with the given key
	 */
	<T extends Control> Value<T> control(ControlKey<T> controlKey);

	/**
	 * @return all available controls
	 */
	Collection<Value<Control>> controls();

	/**
	 * @param controlKey the control key
	 * @return the {@link Value} controlling the keyStroke for the given control
	 * @throws IllegalArgumentException in case no control is associated with the given control key
	 */
	Value<KeyStroke> keyStroke(ControlKey<?> controlKey);

	/**
	 * Returns a {@link KeyEvents.Builder} instance if a keyStroke and a Control is associated with the given {@link ControlKey},
	 * otherwise an empty {@link Optional}.
	 * @param controlKey the key identifying the control
	 * @return a key event builder for the given control
	 */
	Optional<KeyEvents.Builder> keyEvent(ControlKey<?> controlKey);

	/**
	 * @return a copy of this {@link ControlMap} instance
	 */
	ControlMap copy();

	/**
	 * @param controlKeysClass the class containing the control keys
	 * @return a new {@link ControlMap} initialized with control keys found in the given class
	 */
	static ControlMap controlMap(Class<?> controlKeysClass) {
		return new DefaultControlMap(controlKeysClass);
	}
}
