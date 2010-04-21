/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * Provides values mapped to keys.<br>
 * User: Bjorn Darri<br>
 * Date: 4.4.2010<br>
 * Time: 21:06:22<br>
 */
public interface ValueProvider<T, V> {
  public V getValue(final T key);
}
