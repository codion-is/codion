/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import java.util.List;

/**
 * Specifies an object that provides a List of values.
 * @param <T>
 */
public interface ValueListProvider<T> {
  List<T> getValues() throws Exception;
}
