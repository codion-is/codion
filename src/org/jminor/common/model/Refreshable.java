/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * Specifies an object that can be refreshed.
 */
public interface Refreshable {

  /**
   * Performs a refresh
   */
  void refresh();
}
