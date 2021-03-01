/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;

import javax.swing.Icon;
import javax.swing.KeyStroke;

import static java.util.Objects.requireNonNull;

final class ToggleControlBuilder implements ToggleControl.Builder {

  private String name;
  private Value<Boolean> value;
  private StateObserver enabledState;
  private char mnemonic;
  private Icon icon;
  private String description;
  private KeyStroke keyStroke;

  @Override
  public ToggleControl.Builder command(final Control.Command command) {
    throw new UnsupportedOperationException("A ToggleControl can not have a Command");
  }

  @Override
  public ToggleControl.Builder name(final String name) {
    this.name = requireNonNull(name);
    return this;
  }

  @Override
  public ToggleControl.Builder state(final State state) {
    this.value = Value.stateValue(state);
    return this;
  }

  @Override
  public ToggleControl.Builder value(final Value<Boolean> value) {
    this.value = requireNonNull(value);
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
  public ToggleControl.Builder icon(final Icon icon) {
    this.icon = requireNonNull(icon);
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
    final DefaultToggleControl toggleControl = new DefaultToggleControl(name, value, enabledState);
    toggleControl.setMnemonic(mnemonic);
    toggleControl.setIcon(icon);
    toggleControl.setDescription(description);
    toggleControl.setKeyStroke(keyStroke);

    return toggleControl;
  }
}
