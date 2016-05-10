/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * Specifies a simple progress reporting object
 */
public interface ProgressReporter {

  /**
   * Reports the current progress
   * @param currentProgress the current progress
   */
  void reportProgress(final int currentProgress);
}
