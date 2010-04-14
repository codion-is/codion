/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.List;

public interface ValueListProvider<T> {
  List<T> getValueList() throws Exception;
}
