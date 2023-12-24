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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import static java.util.Objects.requireNonNull;

final class ControlBuilder<C extends Control, B extends Control.Builder<C, B>> extends AbstractControlBuilder<C, B> {

  private final Control.Command command;
  private final Control.ActionCommand actionCommand;

  ControlBuilder(Control.ActionCommand actionCommand) {
    this.actionCommand = requireNonNull(actionCommand);
    this.command = null;
  }

  ControlBuilder(Control.Command command) {
    this.command = requireNonNull(command);
    this.actionCommand = null;
  }

  @Override
  protected C createControl() {
    if (command != null) {
      return (C) new DefaultControl(command, name, enabled);
    }
    else {
      return (C) new DefaultActionControl(actionCommand, name, enabled);
    }
  }
}
