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

import is.codion.common.state.StateObserver;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultControl extends AbstractControl {

	private final Command command;
	private final Consumer<Exception> onException;

	DefaultControl(Command command, String name, StateObserver enabled,
								 Consumer<Exception> onException) {
		super(name, enabled);
		this.command = requireNonNull(command);
		this.onException = requireNonNull(onException);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			command.execute();
		}
		catch (Exception exception) {
			onException.accept(exception);
		}
	}

	@Override
	public <C extends Control, B extends Builder<C, B>> Builder<C, B> copy() {
		return (Builder<C, B>) createBuilder(command, null)
						.onException(onException);
	}
}
