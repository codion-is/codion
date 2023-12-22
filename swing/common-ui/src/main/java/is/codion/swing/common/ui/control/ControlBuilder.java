/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
