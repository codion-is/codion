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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.value.Value;

import javax.swing.KeyStroke;

/**
 * Identifies a {@link Control} instance
 * @param <T> the control type
 */
public interface ControlKey<T extends Control> {

	/**
	 * @return the control name
	 */
	String name();

	/**
	 * Note that changing the default keystroke has no effect on already initialized controls.
	 * @return the default keystroke for this shortcut, an empty {@link Value} if none is available
	 */
	Value<KeyStroke> defaultKeystroke();
}
