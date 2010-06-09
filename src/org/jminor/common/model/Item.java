/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.text.Collator;

/**
 * A class encapsulating an item and caption.
 * @param <T> the type of the actual item
 */
public class Item<T> implements Comparable<Item<T>>{

  private static final ThreadLocal<Collator> collator = Util.getThreadLocalCollator();

  private final T item;
  private final String caption;

  public Item(final T item, final String caption) {
    if (caption == null) {
      throw new RuntimeException("Caption is null");
    }
    this.item = item;
    this.caption = caption;
  }

  /**
   * @return the caption
   */
  public String getCaption() {
    return caption;
  }

  /**
   * @return the actual item
   */
  public T getItem() {
    return item;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getCaption();
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof Item && Util.equal(item, ((Item) obj).item);
  }

  @Override
  public int hashCode() {
    return item == null ? 0 : item.hashCode();
  }

  public int compareTo(final Item<T> item) {
    return collator.get().compare(caption, item.caption);
  }
}
