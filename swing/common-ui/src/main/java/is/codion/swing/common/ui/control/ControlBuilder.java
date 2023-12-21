/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class ControlBuilder implements Control.Builder {

  private final Control.Command command;
  private final Control.ActionCommand actionCommand;
  private final Map<String, Object> values = new HashMap<>();

  private String name;
  private StateObserver enabled;
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
  public Control.Builder name(String name) {
    this.name = name;
    return this;
  }

  @Override
  public Control.Builder enabled(StateObserver enabled) {
    this.enabled = enabled;
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
  public Control.Builder value(String key, Object value) {
    if ("enabled".equals(key)) {
      throw new IllegalArgumentException("Can not set the enabled property of a Control");
    }
    values.put(key, value);
    return this;
  }

  @Override
  public Control build() {
    Control control;
    if (command != null) {
      control = new DefaultControl(command, name, enabled);
    }
    else {
      control = new DefaultActionControl(actionCommand, name, enabled);
    }

    control.setMnemonic(mnemonic);
    control.setSmallIcon(smallIcon);
    control.setLargeIcon(largeIcon);
    control.setDescription(description);
    control.setKeyStroke(keyStroke);
    values.forEach(control::putValue);

    return control;
  }
}
