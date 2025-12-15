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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.model.CancelException;
import is.codion.common.utilities.exceptions.Exceptions;

import org.jspecify.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultCommandControl extends AbstractControl implements CommandControl {

	private static final Consumer<Exception> DEFAULT_EXCEPTION_HANDLER = new DefaultExceptionHandler();

	private final @Nullable Command command;
	private final @Nullable ActionCommand actionCommand;
	private final Consumer<Exception> onException;

	private DefaultCommandControl(DefaultCommandControlBuilder builder) {
		super(builder);
		this.command = builder.command;
		this.actionCommand = builder.actionCommand;
		this.onException = builder.onException;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			if (command != null) {
				command.execute();
			}
			else if (actionCommand != null) {
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
		return copyBuilder(requireNonNull(command), null);
	}

	@Override
	public CommandControlBuilder copy(ActionCommand actionCommand) {
		return copyBuilder(null, requireNonNull(actionCommand));
	}

	private CommandControlBuilder copyBuilder(@Nullable Command command, @Nullable ActionCommand actionCommand) {
		CommandControlBuilder builder = new DefaultCommandControlBuilder(command, actionCommand)
						.enabled(enabled())
						.onException(onException);
		keys().forEach(key -> builder.value(key, getValue(key)));

		return builder;
	}

	static final class DefaultCommandControlBuilder extends AbstractControlBuilder<CommandControl, CommandControlBuilder> implements CommandControlBuilder {

		private final @Nullable Command command;
		private final @Nullable ActionCommand actionCommand;

		private Consumer<Exception> onException = DEFAULT_EXCEPTION_HANDLER;

		DefaultCommandControlBuilder(@Nullable Command command, @Nullable ActionCommand actionCommand) {
			if (command == null && actionCommand == null) {
				throw new NullPointerException("Command or ActionCommand must be specified");
			}
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
			return new DefaultCommandControl(this);
		}
	}

	private static final class DefaultExceptionHandler implements Consumer<Exception> {

		@Override
		public void accept(Exception exception) {
			if (exception instanceof CancelException) {
				return; // Operation cancelled
			}
			throw Exceptions.runtime(exception);
		}
	}
}
