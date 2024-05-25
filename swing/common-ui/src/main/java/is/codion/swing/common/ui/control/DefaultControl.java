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

import is.codion.common.event.Event;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultControl extends AbstractControl {

	private final Command command;
	private final ActionCommand actionCommand;
	private final Consumer<Exception> onException;

	private DefaultControl(Command command, ActionCommand actionCommand, DefaultControlBuilder<?, ?> builder) {
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
	public <C extends Control, B extends Builder<C, B>> Builder<C, B> copy() {
		return (Builder<C, B>) createBuilder(command, null);
	}

	@Override
	public <B extends Builder<Control, B>> Builder<Control, B> copy(Command command) {
		return createBuilder(command, null);
	}

	@Override
	public <B extends Builder<Control, B>> Builder<Control, B> copy(ActionCommand actionCommand) {
		return createBuilder(null, actionCommand);
	}

	@Override
	public <B extends Builder<Control, B>> Builder<Control, B> copy(Event<ActionEvent> event) {
		requireNonNull(event);

		return copy(event::accept);
	}

	<B extends Builder<Control, B>> Builder<Control, B> createBuilder(Command command, ActionCommand actionCommand) {
		if (command == null && actionCommand == null) {
			throw new NullPointerException("Command or ActionCommand must be specified");
		}
		B builder = new DefaultControl.DefaultControlBuilder<Control, B>(command, actionCommand)
						.enabled(enabled())
						.onException(onException);
		keys().forEach(key -> builder.value(key, getValue(key)));

		return builder;
	}

	static final class DefaultControlBuilder<C extends Control, B extends Builder<C, B>> extends AbstractControlBuilder<C, B> {

		private final Command command;
		private final ActionCommand actionCommand;

		DefaultControlBuilder(Command command, ActionCommand actionCommand) {
			this.command = command;
			this.actionCommand = actionCommand;
		}

		@Override
		public C build() {
			return (C) new DefaultControl(command, actionCommand, this);
		}
	}
}
