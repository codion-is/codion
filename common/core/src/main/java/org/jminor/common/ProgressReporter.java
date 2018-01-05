/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

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
