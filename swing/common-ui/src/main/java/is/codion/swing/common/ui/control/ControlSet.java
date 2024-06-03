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
public interface ControlSet {

	/**
	 * @param controlId the control id
	 * @return the {@link Value} specifying the {@link Control} associated with the given id
	 * @param <T> the control type
	 * @throws IllegalArgumentException in case no control is associated with the given shortcut
	 */
	<T extends Control> Value<T> control(ControlId<T> controlId);

	/**
	 * @return all available controls
	 */
	Collection<Value<Control>> controls();

	/**
	 * @param controlIdsClass the class containing the control ids
	 * @return a new {@link ControlSet} initialized with control ids found in the given class
	 */
	static ControlSet controlSet(Class<?> controlIdsClass) {
		return new DefaultControlSet(controlIdsClass);
	}
}
