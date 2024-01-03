/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

/**
 * Specifies when a text field based component value should update the underlying value.
 */
public enum UpdateOn {
  /**
   * Update when field loses focus.
   */
  FOCUS_LOST,
  /**
   * Update each time the text field value changes.
   */
  VALUE_CHANGE
}
