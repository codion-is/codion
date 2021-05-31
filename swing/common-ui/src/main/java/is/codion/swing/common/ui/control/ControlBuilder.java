/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Icon;
import javax.swing.KeyStroke;

import static java.util.Objects.requireNonNull;

final class ControlBuilder implements Control.Builder {

  private final Control.Command command;
  private final Control.ActionCommand actionCommand;

  private String caption;
  private StateObserver enabledState;
  private char mnemonic;
  private Icon icon;
  private String description;
  private KeyStroke keyStroke;

  ControlBuilder(final Control.ActionCommand actionCommand) {
    this.actionCommand = requireNonNull(actionCommand);
    this.command = null;
  }

  ControlBuilder(final Control.Command command) {
    this.command = requireNonNull(command);
    this.actionCommand = null;
  }

  @Override
  public Control.Builder caption(final String caption) {
    this.caption = caption;
    return this;
  }

  @Override
  public Control.Builder enabledState(final StateObserver enabledState) {
    this.enabledState = enabledState;
    return this;
  }

  @Override
  public Control.Builder mnemonic(final char mnemonic) {
    this.mnemonic = mnemonic;
    return this;
  }

  @Override
  public Control.Builder icon(final Icon icon) {
    this.icon = icon;
    return this;
  }

  @Override
  public Control.Builder description(final String description) {
    this.description = description;
    return this;
  }

  @Override
  public Control.Builder keyStroke(final KeyStroke keyStroke) {
    this.keyStroke = keyStroke;
    return this;
  }

  @Override
  public Control build() {
    final Control control;
    if (command != null) {
      control = new DefaultControl(command, caption, enabledState);
    }
    else {
      control = new DefaultActionControl(actionCommand, caption, enabledState);
    }

    return control.setMnemonic(mnemonic)
            .setIcon(icon)
            .setDescription(description)
            .setKeyStroke(keyStroke);
  }
}
