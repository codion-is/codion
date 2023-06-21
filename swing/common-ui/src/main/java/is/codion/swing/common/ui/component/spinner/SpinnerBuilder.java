/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.JSpinner;

/**
 * A builder for JSpinner.
 */
public interface SpinnerBuilder<T, B extends SpinnerBuilder<T, B>> extends ComponentBuilder<T, JSpinner, B> {

  /**
   * @param editable false if the spinner field should not be editable
   * @return this builder instance
   */
  B editable(boolean editable);

  /**
   * @param columns the number of colums in the spinner text component
   * @return this builder instance
   */
  B columns(int columns);

  /**
   * Enable mouse wheel scrolling on the spinner
   * @param mouseWheelScrolling true if mouse wheel scrolling should be enabled
   * @return this builder instance
   */
  B mouseWheelScrolling(boolean mouseWheelScrolling);

  /**
   * Enable reversed mouse wheel scrolling on the spinner
   * @param mouseWheelScrollingReversed if true then up/away decreases the value and down/towards increases it.
   * @return this builder instance
   */
  B mouseWheelScrollingReversed(boolean mouseWheelScrollingReversed);

  /**
   * @param horizontalAlignment the horizontal text alignment
   * @return this builder instance
   */
  B horizontalAlignment(int horizontalAlignment);
}
