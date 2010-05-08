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
  Collection<V> getValues();
}
