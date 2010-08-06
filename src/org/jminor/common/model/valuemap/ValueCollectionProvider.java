/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import java.util.Collection;

/**
 * Specifies an object that provides a Collection of values.
 * @param <V> the type of the values
 */
public interface ValueCollectionProvider<V> {

  /**
   * Retrieves the values associated with this value provider.
   * @return a collection containing the values provided by this value provider
   */
  Collection<V> getValues();
}
