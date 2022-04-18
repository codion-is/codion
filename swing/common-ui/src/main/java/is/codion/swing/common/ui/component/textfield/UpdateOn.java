/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

/**
 * Specifies when a text field based component value should update the underlying value.
 */
public enum UpdateOn {
  /**
   * Update when field loses focus.
   */
  FOCUS_LOST,
  /**
   * Update on each keystroke.
   */
  KEYSTROKE
}
