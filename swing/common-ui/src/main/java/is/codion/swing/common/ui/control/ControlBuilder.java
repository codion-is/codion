/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
  private Icon smallIcon;
  private Icon largeIcon;
  private String description;
  private KeyStroke keyStroke;

  ControlBuilder(Control.ActionCommand actionCommand) {
    this.actionCommand = requireNonNull(actionCommand);
    this.command = null;
  }

  ControlBuilder(Control.Command command) {
    this.command = requireNonNull(command);
    this.actionCommand = null;
  }

  @Override
  public Control.Builder caption(String caption) {
    this.caption = caption;
    return this;
  }

  @Override
  public Control.Builder enabledState(StateObserver enabledState) {
    this.enabledState = enabledState;
    return this;
  }

  @Override
  public Control.Builder mnemonic(char mnemonic) {
    this.mnemonic = mnemonic;
    return this;
  }

  @Override
  public Control.Builder smallIcon(Icon smallIcon) {
    this.smallIcon = smallIcon;
    return this;
  }

  @Override
  public Control.Builder largeIcon(Icon largeIcon) {
    this.largeIcon = largeIcon;
    return this;
  }

  @Override
  public Control.Builder description(String description) {
    this.description = description;
    return this;
  }

  @Override
  public Control.Builder keyStroke(KeyStroke keyStroke) {
    this.keyStroke = keyStroke;
    return this;
  }

  @Override
  public Control build() {
    Control control;
    if (command != null) {
      control = new DefaultControl(command, caption, enabledState);
    }
    else {
      control = new DefaultActionControl(actionCommand, caption, enabledState);
    }

    return control.setMnemonic(mnemonic)
            .setSmallIcon(smallIcon)
            .setLargeIcon(largeIcon)
            .setDescription(description)
            .setKeyStroke(keyStroke);
  }
}
