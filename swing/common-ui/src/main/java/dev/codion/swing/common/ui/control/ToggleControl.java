/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.control;

import dev.codion.common.value.Value;

/**
 * A Control for toggling a boolean value.
 */
public interface ToggleControl extends Control {

  /**
   * @return the value being toggled by this toggle control
   */
  Value<Boolean> getValue();
}
