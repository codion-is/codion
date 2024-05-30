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

import java.util.function.Consumer;

/**
 * A {@link Control} based on a {@link is.codion.swing.common.ui.control.Control.Command}
 */
public interface CommandControl extends Control {

	/**
	 * Returns a new {@link CommandControlBuilder} instance, based on a copy of this control, using the given command.
	 * @param command the command for the resulting control
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
	 * Builds a {@link CommandControl}
	 */
	interface CommandControlBuilder extends Builder<CommandControl, CommandControlBuilder> {

		/**
		 * @param onException the exception handler for this control
		 * @return this builder
		 */
		CommandControlBuilder onException(Consumer<Exception> onException);
	}
}
