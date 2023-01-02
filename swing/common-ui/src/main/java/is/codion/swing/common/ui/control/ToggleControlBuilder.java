/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;

import javax.swing.Icon;
import javax.swing.KeyStroke;

import static java.util.Objects.requireNonNull;

final class ToggleControlBuilder implements ToggleControl.Builder {

  private final Value<Boolean> value;

  private String caption;
  private StateObserver enabledState;
  private char mnemonic;
  private Icon smallIcon;
  private Icon largeIcon;
  private String description;
  private KeyStroke keyStroke;

  ToggleControlBuilder(Value<Boolean> value) {
    this.value = requireNonNull(value);
  }

  @Override
  public ToggleControl.Builder caption(String caption) {
    this.caption = requireNonNull(caption);
    return this;
  }

  @Override
  public ToggleControl.Builder enabledState(StateObserver enabledState) {
    this.enabledState = requireNonNull(enabledState);
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
    this.description = requireNonNull(description);
    return this;
  }

  @Override
  public ToggleControl.Builder keyStroke(KeyStroke keyStroke) {
    this.keyStroke = requireNonNull(keyStroke);
    return this;
  }

  @Override
  public ToggleControl build() {
    DefaultToggleControl toggleControl = new DefaultToggleControl(caption, value, enabledState);
    toggleControl.setMnemonic(mnemonic);
    toggleControl.setSmallIcon(smallIcon);
    toggleControl.setLargeIcon(largeIcon);
    toggleControl.setDescription(description);
    toggleControl.setKeyStroke(keyStroke);

    return toggleControl;
  }
}
