/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JProgressBar;

/**
 * Builds a JProgressBar.
 */
public interface ProgressBarBuilder extends ComponentBuilder<Integer, JProgressBar, ProgressBarBuilder> {

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
   * @return a new JProgressBar
   */
  JProgressBar build();
}
