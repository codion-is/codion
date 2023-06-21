/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.progressbar;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.BoundedRangeModel;
import javax.swing.JProgressBar;

/**
 * Builds a JProgressBar.
 */
public interface ProgressBarBuilder extends ComponentBuilder<Integer, JProgressBar, ProgressBarBuilder> {

  /**
   * @param string a string to paint
   * @return this builder
   * @see JProgressBar#setString(String)
   */
  ProgressBarBuilder string(String string);

  /**
   * @param borderPainted true if a border should be painted
   * @return this builder
   * @see JProgressBar#setBorderPainted(boolean)
   */
  ProgressBarBuilder borderPainted(boolean borderPainted);

  /**
   * @param stringPainted true if a progress string should be painted
   * @return this builder
   * @see JProgressBar#setStringPainted(boolean)
   */
  ProgressBarBuilder stringPainted(boolean stringPainted);

  /**
   * @param orientation the orientiation
   * @return this builder
   * @see JProgressBar#setOrientation(int)
   */
  ProgressBarBuilder orientation(int orientation);

  /**
   * @param indeterminate true if the progress bar should be inditerminate
   * @return this builder
   * @see JProgressBar#setIndeterminate(boolean)
   */
  ProgressBarBuilder indeterminate(boolean indeterminate);

  /**
   * @return a new JProgressBar
   */
  JProgressBar build();

  /**
   * @param boundedRangeModel the progress bar model
   * @return a new {@link ProgressBarBuilder} instance
   */
  static ProgressBarBuilder builder(BoundedRangeModel boundedRangeModel) {
    return new DefaultProgressBarBuilder(boundedRangeModel);
  }
}
