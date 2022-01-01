/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.checkbox.NullableCheckBox;

import javax.swing.JCheckBox;

/**
 * Builds a JCheckBox.
 */
public interface CheckBoxBuilder extends ToggleButtonBuilder<JCheckBox, CheckBoxBuilder> {

  /**
   * @param nullable if true then a {@link NullableCheckBox} is built.
   * @return this builder instance
   */
  CheckBoxBuilder nullable(boolean nullable);

  /**
   * @param horizontalAlignment the horizontal text alignment
   * @return this builder instance
   */
  CheckBoxBuilder horizontalAlignment(int horizontalAlignment);
}
