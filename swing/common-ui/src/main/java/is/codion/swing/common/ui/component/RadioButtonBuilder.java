/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JRadioButton;

/**
 * Builds a JRadioButton.
 */
public interface RadioButtonBuilder extends ButtonBuilder<Boolean, JRadioButton, RadioButtonBuilder> {

  /**
   * @param horizontalAlignment the horizontal alignment
   * @return this builder instance
   */
  RadioButtonBuilder horizontalAlignment(int horizontalAlignment);
}
