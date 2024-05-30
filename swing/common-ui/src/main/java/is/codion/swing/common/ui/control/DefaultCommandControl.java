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

import is.codion.common.model.CancelException;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultCommandControl extends AbstractControl implements CommandControl {

	private static final Consumer<Exception> DEFAULT_EXCEPTION_HANDLER = new DefaultExceptionHandler();

	private final Command command;
	private final ActionCommand actionCommand;
	private final Consumer<Exception> onException;

	private DefaultCommandControl(Command command, ActionCommand actionCommand, DefaultCommandControlBuilder builder) {
		super(builder);
		this.command = command;
		this.actionCommand = actionCommand;
		this.onException = builder.onException;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			if (command != null) {
				command.execute();
			}
			else {
				actionCommand.execute(e);
			}
		}
		catch (Exception exception) {
			onException.accept(exception);
		}
	}

	@Override
	public CommandControlBuilder copy() {
		return copyBuilder(command, actionCommand);
	}

	@Override
	public CommandControlBuilder copy(Command command) {
		return copyBuilder(command, null);
	}

	@Override
	public CommandControlBuilder copy(ActionCommand actionCommand) {
		return copyBuilder(null, actionCommand);
	}

	private CommandControlBuilder copyBuilder(Command command, ActionCommand actionCommand) {
		if (command == null && actionCommand == null) {
			throw new NullPointerException("Command or ActionCommand must be specified");
		}
		CommandControlBuilder builder = new DefaultCommandControlBuilder(command, actionCommand)
						.enabled(enabled())
						.onException(onException);
		keys().forEach(key -> builder.value(key, getValue(key)));

		return builder;
	}

	static final class DefaultCommandControlBuilder extends AbstractControlBuilder<CommandControl, CommandControlBuilder> implements CommandControlBuilder {

		private final Command command;
		private final ActionCommand actionCommand;

		private Consumer<Exception> onException = DEFAULT_EXCEPTION_HANDLER;

		DefaultCommandControlBuilder(Command command, ActionCommand actionCommand) {
			this.command = command;
			this.actionCommand = actionCommand;
		}

		@Override
		public CommandControlBuilder onException(Consumer<Exception> onException) {
			this.onException = requireNonNull(onException);
			return self();
		}

		@Override
		public CommandControl build() {
			return new DefaultCommandControl(command, actionCommand, this);
		}
	}

	private static final class DefaultExceptionHandler implements Consumer<Exception> {

		@Override
		public void accept(Exception exception) {
			if (exception instanceof CancelException) {
				return; // Operation cancelled
			}
			if (exception instanceof RuntimeException) {
				throw (RuntimeException) exception;
			}

			throw new RuntimeException(exception);
		}
	}
}
