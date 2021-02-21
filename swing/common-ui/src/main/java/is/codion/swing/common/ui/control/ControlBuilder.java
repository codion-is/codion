/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Icon;
import javax.swing.KeyStroke;

import static java.util.Objects.requireNonNull;

final class ControlBuilder implements Control.Builder {

  private Control.Command command;
  private String name;
  private StateObserver enabledState;
  private char mnemonic;
  private Icon icon;
  private String description;
  private KeyStroke keyStroke;

  @Override
  public Control.Builder command(final Control.Command command) {
    this.command = requireNonNull(command);
    return this;
  }

  @Override
  public Control.Builder name(final String name) {
    this.name = name;
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
    if (command == null) {
      throw new IllegalStateException("Command must be specified for control");
    }

    return new DefaultControl(command, name, enabledState)
            .setMnemonic(mnemonic)
            .setIcon(icon)
            .setDescription(description)
            .setKeyStroke(keyStroke);
  }
}
