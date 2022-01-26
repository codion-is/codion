/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  private String description;
  private KeyStroke keyStroke;

  ToggleControlBuilder(final Value<Boolean> value) {
    this.value = requireNonNull(value);
  }

  @Override
  public ToggleControl.Builder caption(final String caption) {
    this.caption = requireNonNull(caption);
    return this;
  }

  @Override
  public ToggleControl.Builder enabledState(final StateObserver enabledState) {
    this.enabledState = requireNonNull(enabledState);
    return this;
  }

  @Override
  public ToggleControl.Builder mnemonic(final char mnemonic) {
    this.mnemonic = mnemonic;
    return this;
  }

  @Override
  public ToggleControl.Builder smallIcon(final Icon smallIcon) {
    this.smallIcon = requireNonNull(smallIcon);
    return this;
  }

  @Override
  public ToggleControl.Builder description(final String description) {
    this.description = requireNonNull(description);
    return this;
  }

  @Override
  public ToggleControl.Builder keyStroke(final KeyStroke keyStroke) {
    this.keyStroke = requireNonNull(keyStroke);
    return this;
  }

  @Override
  public ToggleControl build() {
    final DefaultToggleControl toggleControl = new DefaultToggleControl(caption, value, enabledState);
    toggleControl.setMnemonic(mnemonic);
    toggleControl.setSmallIcon(smallIcon);
    toggleControl.setDescription(description);
    toggleControl.setKeyStroke(keyStroke);

    return toggleControl;
  }
}
