/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * A class encapsulating an item and caption.
 * @param <T> the type of the actual item
 */
public final class Item<T> implements Comparable<Item>, Serializable {

  private static final long serialVersionUID = 1;

  private final T item;
  private final String caption;

  private transient Comparator<String> collator = TextUtil.getSpaceAwareCollator();

  /**
   * Instantiates a new Item, with the caption as item.toString(),
   * zero length string in case of a null item
   * @param item the item, may be null
   */
  public Item(final T item) {
    this(item, item == null ? "" : item.toString());
  }

  /**
   * Instantiates a new Item.
   * @param item the item, may be null
   * @param caption the caption
   * @throws NullPointerException if caption is null
   */
  public Item(final T item, final String caption) {
    this.item = item;
    this.caption = Objects.requireNonNull(caption, "caption");
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

  /**
   * @return the item caption
   */
  @Override
  public String toString() {
    return caption;
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object obj) {
    return obj instanceof Item && Objects.equals(item, ((Item) obj).item);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return item == null ? 0 : item.hashCode();
  }

  /**
   * Compares this item with the given item according to the caption.
   * @param o the item to compare with
   * @return the compare result
   */
  @Override
  public int compareTo(final Item o) {
    return collator.compare(caption, o.caption);
  }

  private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
    collator = TextUtil.getSpaceAwareCollator();
  }
}
