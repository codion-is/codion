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

import org.jspecify.annotations.Nullable;

import javax.swing.KeyStroke;
import java.util.function.Consumer;

/**
 * A {@link Control} based on a {@link is.codion.swing.common.ui.control.Control.Command}
 */
public interface CommandControl extends Control {

	/**
	 * Returns a new {@link CommandControlBuilder} instance, based on a copy of this control.
	 * @return a new builder
	 */
	@Override
	CommandControlBuilder copy();

	/**
	 * Returns a new {@link CommandControlBuilder} instance, based on a copy of this control, using the given command.
	 * @param command the command for the resulting control
	 * @return a new builder
	 */
	CommandControlBuilder copy(Command command);

	/**
	 * Returns a new {@link CommandControlBuilder} instance, based on a copy of this control, using the given command.
	 * @param actionCommand the command for the resulting control
	 * @return a new builder
	 */
	CommandControlBuilder copy(ActionCommand actionCommand);

	/**
	 * @param name the control name
	 * @return a new {@link ControlKey} for identifying a {@link CommandControl} instance
	 */
	static ControlKey<CommandControl> key(String name) {
		return key(name, null);
	}

	/**
	 * @param name the control name
	 * @param defaultKeyStroke the default keystroke
	 * @return a new {@link ControlKey} for identifying a {@link CommandControl} instance
	 */
	static ControlKey<CommandControl> key(String name, @Nullable KeyStroke defaultKeyStroke) {
		return new DefaultControlKey<>(name, defaultKeyStroke);
	}

	/**
	 * Builds a {@link CommandControl}
	 */
	interface CommandControlBuilder extends ControlBuilder<CommandControl, CommandControlBuilder> {

		/**
		 * @param onException the exception handler for this control
		 * @return this builder
		 */
		CommandControlBuilder onException(Consumer<Exception> onException);
	}
}
