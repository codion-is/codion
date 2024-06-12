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
public interface ControlKey<T extends Control> {

	/**
	 * @return the class of the control identified by this key
	 */
	Class<T> controlClass();

	/**
	 * @return the default keystroke for this shortcut, an empty Optional if none is available
	 */
	Optional<KeyStroke> defaultKeystroke();

	/**
	 * @return a new {@link ControlKey} for identifying a {@link CommandControl} instance
	 */
	static ControlKey<CommandControl> commandControl() {
		return commandControl(null);
	}

	/**
	 * @param defaultKeyStroke the default keystroke
	 * @return a new {@link ControlKey} for identifying a {@link CommandControl} instance
	 */
	static ControlKey<CommandControl> commandControl(KeyStroke defaultKeyStroke) {
		return new DefaultControlKey<>(CommandControl.class, defaultKeyStroke);
	}

	/**
	 * @return a new {@link ControlKey} for identifying a {@link CommandControl} instance
	 */
	static ControlKey<ToggleControl> toggleControl() {
		return toggleControl(null);
	}

	/**
	 * @param defaultKeyStroke the default keystroke
	 * @return a new {@link ControlKey} for identifying a {@link CommandControl} instance
	 */
	static ControlKey<ToggleControl> toggleControl(KeyStroke defaultKeyStroke) {
		return new DefaultControlKey<>(ToggleControl.class, defaultKeyStroke);
	}

	/**
	 * @return a new {@link ControlKey} for identifying a {@link Controls} instance
	 */
	static ControlKey<Controls> controls() {
		return controls(null);
	}

	/**
	 * @param defaultKeyStroke the default keystroke
	 * @return a new {@link ControlKey} for identifying a {@link Controls} instance
	 */
	static ControlKey<Controls> controls(KeyStroke defaultKeyStroke) {
		return new DefaultControlKey<>(Controls.class, defaultKeyStroke);
	}
}
