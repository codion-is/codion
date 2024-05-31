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

import javax.swing.KeyStroke;
import java.util.Optional;

/**
 * Identifies a {@link Control} instance
 * @param <T> the control type
 */
public interface ControlId<T extends Control> {

	/**
	 * @return the class of the control identified by this identifier
	 */
	Class<T> controlClass();

	/**
	 * @return the default keystroke for this shortcut, an empty Optional if none is available
	 */
	Optional<KeyStroke> defaultKeystroke();

	/**
	 * @return a new {@link ControlId} for identifying a {@link CommandControl} instance
	 */
	static ControlId<CommandControl> commandControl() {
		return commandControl(null);
	}

	/**
	 * @return a new {@link ControlId} for identifying a {@link CommandControl} instance
	 * @param defaultKeyStroke the default keystroke
	 */
	static ControlId<CommandControl> commandControl(KeyStroke defaultKeyStroke) {
		return new DefaultControlId<>(CommandControl.class, defaultKeyStroke);
	}

	/**
	 * @return a new {@link ControlId} for identifying a {@link CommandControl} instance
	 */
	static ControlId<ToggleControl> toggleControl() {
		return toggleControl(null);
	}

	/**
	 * @return a new {@link ControlId} for identifying a {@link CommandControl} instance
	 * @param defaultKeyStroke the default keystroke
	 */
	static ControlId<ToggleControl> toggleControl(KeyStroke defaultKeyStroke) {
		return new DefaultControlId<>(ToggleControl.class, defaultKeyStroke);
	}

	/**
	 * @return a new {@link ControlId} for identifying a {@link Controls} instance
	 */
	static ControlId<Controls> controls() {
		return controls(null);
	}

	/**
	 * @return a new {@link ControlId} for identifying a {@link Controls} instance
	 * @param defaultKeyStroke the default keystroke
	 */
	static ControlId<Controls> controls(KeyStroke defaultKeyStroke) {
		return new DefaultControlId<>(Controls.class, defaultKeyStroke);
	}
}
