/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class ToggleControlBuilder implements ToggleControl.Builder {

  private final Value<Boolean> value;
  private final Map<String, Object> values = new HashMap<>();

  private String name;
  private StateObserver enabled;
  private char mnemonic;
  private Icon smallIcon;
  private Icon largeIcon;
  private String description;
  private KeyStroke keyStroke;

  ToggleControlBuilder(Value<Boolean> value) {
    this.value = requireNonNull(value);
  }

  @Override
  public ToggleControl.Builder name(String name) {
    this.name = name;
    return this;
  }

  @Override
  public ToggleControl.Builder enabled(StateObserver enabled) {
    this.enabled = enabled;
    return this;
  }

  @Override
  public ToggleControl.Builder mnemonic(char mnemonic) {
    this.mnemonic = mnemonic;
    return this;
  }

  @Override
  public ToggleControl.Builder smallIcon(Icon smallIcon) {
    this.smallIcon = smallIcon;
    return this;
  }

  @Override
  public ToggleControl.Builder largeIcon(Icon largeIcon) {
    this.largeIcon = largeIcon;
    return this;
  }

  @Override
  public ToggleControl.Builder description(String description) {
    this.description = description;
    return this;
  }

  @Override
  public ToggleControl.Builder keyStroke(KeyStroke keyStroke) {
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
  public ToggleControl build() {
    DefaultToggleControl toggleControl = new DefaultToggleControl(value, name, enabled);
    toggleControl.setMnemonic(mnemonic);
    toggleControl.setSmallIcon(smallIcon);
    toggleControl.setLargeIcon(largeIcon);
    toggleControl.setDescription(description);
    toggleControl.setKeyStroke(keyStroke);
    values.forEach(toggleControl::putValue);

    return toggleControl;
  }
}
