/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.io.Serializable;
import java.text.Collator;

/**
 * A class encapsulating an item and caption.
 * @param <T> the type of the actual item
 */
public class Item<T> implements Comparable<Item>, Serializable {

  private static final long serialVersionUID = 1;

  private static final ThreadLocal<Collator> COLLATOR = Util.getThreadLocalCollator();

  private final T item;
  private final String caption;

  /**
   * Instantiates a new Item.
   * @param item the item
   * @param caption the caption
   */
  public Item(final T item, final String caption) {
    Util.rejectNullValue(caption, "caption");
    this.item = item;
    this.caption = caption;
  }

  /**
   * @return the caption
   */
  public final String getCaption() {
    return caption;
  }

  /**
   * @return the actual item
   */
  public final T getItem() {
    return item;
  }

  /**
   * @return the item caption
   */
  @Override
  public final String toString() {
    return caption;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof Item && Util.equal(item, ((Item) obj).item);
  }

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    return item == null ? 0 : item.hashCode();
  }

  /**
   * Compares this item with the given item according to the caption.
   * @param o the item to compare with
   * @return the compare result
   */
  public final int compareTo(final Item o) {
    return COLLATOR.get().compare(caption, o.caption);
  }
}
