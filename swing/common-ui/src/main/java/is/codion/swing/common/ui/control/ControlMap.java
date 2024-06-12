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
package is.codion.swing.common.ui.control;

import is.codion.common.value.Value;

import java.util.Collection;

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
	 * @param controlKeysClass the class containing the control keys
	 * @return a new {@link ControlMap} initialized with control keys found in the given class
	 */
	static ControlMap controlMap(Class<?> controlKeysClass) {
		return new DefaultControlMap(controlKeysClass);
	}
}
