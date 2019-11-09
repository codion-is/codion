/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A class encapsulating a value and a caption representing the value.
 * @param <T> the type of the value
 */
public final class Item<T> implements Comparable<Item>, Serializable {

  private static final long serialVersionUID = 1;

  private final T value;
  private final String caption;

  private transient Comparator<String> collator;

  /**
   * Instantiates a new Item, with the caption as item.toString() or an empty string in case of a null value
   * @param value the value, may be null
   */
  public Item(final T value) {
    this(value, value == null ? "" : value.toString());
  }

  /**
   * Instantiates a new Item.
   * @param value the value, may be null
   * @param caption the caption
   * @throws NullPointerException if caption is null
   */
  public Item(final T value, final String caption) {
    this.value = value;
    this.caption = requireNonNull(caption, "caption");
  }

  /**
   * @return the caption
   */
  public String getCaption() {
    return caption;
  }

  /**
   * @return the value
   */
  public T getValue() {
    return value;
  }

  /**
   * @return the caption
   */
  @Override
  public String toString() {
    return caption;
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object obj) {
    return obj instanceof Item && Objects.equals(value, ((Item) obj).value);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return value == null ? 0 : value.hashCode();
  }

  /**
   * Compares this items caption with the caption of the given item
   * @param item the item to compare with
   * @return the compare result
   */
  @Override
  public int compareTo(final Item item) {
    return getCollator().compare(caption, item.caption);
  }

  private Comparator<String> getCollator() {
    if (collator == null) {
      collator = TextUtil.getSpaceAwareCollator();
    }

    return collator;
  }
}
