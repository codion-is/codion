/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.util.HashMap;
import java.util.Map;

abstract class AbstractControlBuilder<C extends Control, B extends Control.Builder<C, B>> implements Control.Builder<C, B> {

  private final Map<String, Object> values = new HashMap<>();

  protected String name;
  protected StateObserver enabled;
  private char mnemonic;
  private Icon smallIcon;
  private Icon largeIcon;
  private String description;
  private KeyStroke keyStroke;

  @Override
  public final B name(String name) {
    this.name = name;
    return (B) this;
  }

  @Override
  public final B enabled(StateObserver enabled) {
    this.enabled = enabled;
    return (B) this;
  }

  @Override
  public final B mnemonic(char mnemonic) {
    this.mnemonic = mnemonic;
    return (B) this;
  }

  @Override
  public final B smallIcon(Icon smallIcon) {
    this.smallIcon = smallIcon;
    return (B) this;
  }

  @Override
  public final B largeIcon(Icon largeIcon) {
    this.largeIcon = largeIcon;
    return (B) this;
  }

  @Override
  public final B description(String description) {
    this.description = description;
    return (B) this;
  }

  @Override
  public final B keyStroke(KeyStroke keyStroke) {
    this.keyStroke = keyStroke;
    return (B) this;
  }

  @Override
  public final B value(String key, Object value) {
    if ("enabled".equals(key)) {
      throw new IllegalArgumentException("Can not set the enabled property of a Control");
    }
    values.put(key, value);
    return (B) this;
  }

  @Override
  public C build() {
    C control = createControl();
    control.setMnemonic(mnemonic);
    control.setSmallIcon(smallIcon);
    control.setLargeIcon(largeIcon);
    control.setDescription(description);
    control.setKeyStroke(keyStroke);
    values.forEach(control::putValue);

    return control;
  }

  protected abstract C createControl();
}
