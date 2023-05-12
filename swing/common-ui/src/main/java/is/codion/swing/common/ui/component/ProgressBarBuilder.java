/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JProgressBar;

/**
 * Builds a JProgressBar.
 */
public interface ProgressBarBuilder extends ComponentBuilder<Integer, JProgressBar, ProgressBarBuilder> {

  /**
   * @param string a string to paint
   * @return this builder
   */
  ProgressBarBuilder string(String string);

  /**
   * @param borderPainted true if a border should be painted
   * @return this builder
   */
  ProgressBarBuilder borderPainted(boolean borderPainted);

  /**
   * @param stringPainted true if a progress string should be painted
   * @return this builder
   */
  ProgressBarBuilder stringPainted(boolean stringPainted);

  /**
   * @param orientation the orientiation
   * @return this builder
   */
  ProgressBarBuilder orientation(int orientation);

  /**
   * @param indeterminate true if the progress bar should be inditerminate
   * @return this builder
   */
  ProgressBarBuilder indeterminate(boolean indeterminate);

  /**
   * @return a new JProgressBar
   */
  JProgressBar build();
}
