/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.state.StateObserver;
import org.jminor.common.value.Value;

import static java.util.Objects.requireNonNull;

/**
 * A Control for toggling a boolean value
 */
public final class ToggleControl extends Control {

  private final Value<Boolean> value;

  /**
   * @param name the name
   * @param value the value to toggle
   * @param enabledObserver an observer indicating when this control should be enabled
   */
  ToggleControl(final String name, final Value<Boolean> value, final StateObserver enabledObserver) {
    super(name, enabledObserver);
    this.value = requireNonNull(value, "value");
  }

  /**
   * @return the value being toggled by this toggle control
   */
  public Value<Boolean> getValue() {
    return value;
  }
}
