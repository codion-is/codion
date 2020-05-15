/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.item;

import java.io.Serializable;

/**
 * A class encapsulating a constant value and a caption representing the value.
 * Comparing Items is based on their caption.
 * @param <T> the type of the value
 */
public interface Item<T> extends Comparable<Item<T>>, Serializable {

  /**
   * @return the caption
   */
  String getCaption();

  /**
   * @return the value
   */
  T getValue();
}
