/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.control;

import dev.codion.common.state.StateObserver;
import dev.codion.common.value.Value;

import java.awt.event.ActionEvent;

import static java.util.Objects.requireNonNull;

/**
 * A default ToggleControl implementation.
 */
final class DefaultToggleControl extends AbstractControl implements ToggleControl {

  private final Value<Boolean> value;

  /**
   * @param name the name
   * @param value the value to toggle
   * @param enabledObserver an observer indicating when this control should be enabled
   */
  DefaultToggleControl(final String name, final Value<Boolean> value, final StateObserver enabledObserver) {
    super(name, enabledObserver);
    this.value = requireNonNull(value, "value");
  }

  @Override
  public Value<Boolean> getValue() {
    return value;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {/*Not required*/}
}
