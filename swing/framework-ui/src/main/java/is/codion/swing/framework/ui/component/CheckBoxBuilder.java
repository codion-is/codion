/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.swing.common.ui.checkbox.NullableCheckBox;

import javax.swing.JCheckBox;

/**
 * Builds a JCheckBox.
 */
public interface CheckBoxBuilder extends ComponentBuilder<Boolean, JCheckBox, CheckBoxBuilder> {

  /**
   * @param includeCaption specifies whether a caption should be included
   * @return this builder instance
   */
  CheckBoxBuilder includeCaption(boolean includeCaption);

  /**
   * @param nullable if true then a {@link NullableCheckBox} is built.
   * @return this builder instance
   */
  CheckBoxBuilder nullable(boolean nullable);
}
