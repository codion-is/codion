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

import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.control.CommandControl.CommandControlBuilder;
import is.codion.swing.common.ui.control.Control.ActionCommand;
import is.codion.swing.common.ui.control.Control.BuilderFactory;
import is.codion.swing.common.ui.control.Control.Command;
import is.codion.swing.common.ui.control.DefaultCommandControl.DefaultCommandControlBuilder;
import is.codion.swing.common.ui.control.DefaultToggleControl.DefaultToggleControlBuilder;
import is.codion.swing.common.ui.control.ToggleControl.ToggleControlBuilder;

import static java.util.Objects.requireNonNull;

final class DefaultControlBuilderFactory implements BuilderFactory {

	@Override
	public CommandControlBuilder command(Command command) {
		return new DefaultCommandControlBuilder(requireNonNull(command), null);
	}

	@Override
	public CommandControlBuilder action(ActionCommand actionCommand) {
		return new DefaultCommandControlBuilder(null, requireNonNull(actionCommand));
	}

	@Override
	public ToggleControlBuilder toggle(Value<Boolean> value) {
		return new DefaultToggleControlBuilder(value);
	}

	@Override
	public ToggleControlBuilder toggle(State state) {
		return new DefaultToggleControlBuilder(state);
	}
}
