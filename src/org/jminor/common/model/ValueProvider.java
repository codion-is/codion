/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * Provides values mapped to keys.
 * User: Bjorn Darri
 * Date: 4.4.2010
 * Time: 21:06:22
 */
public interface ValueProvider {
  public Object getValue(final Object key);
}
