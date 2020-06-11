/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.item;

import is.codion.common.Text;

import java.util.Comparator;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class DefaultItem<T> implements Item<T> {

  private static final long serialVersionUID = 1;

  private final T value;
  private final String caption;

  private transient Comparator<String> collator;

  /**
   * Instantiates a new Item.
   * @param value the value, may be null
   * @param caption the caption
   * @throws NullPointerException if caption is null
   */
  DefaultItem(final T value, final String caption) {
    this.value = value;
    this.caption = requireNonNull(caption, "caption");
  }

  /**
   * @return the caption
   */
  @Override
  public String getCaption() {
    return caption;
  }

  /**
   * @return the value
   */
  @Override
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

  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof Item && Objects.equals(value, ((Item<T>) obj).getValue());
  }

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
  public int compareTo(final Item<T> item) {
    return getCollator().compare(caption, item.getCaption());
  }

  private Comparator<String> getCollator() {
    if (collator == null) {
      collator = Text.getSpaceAwareCollator();
    }

    return collator;
  }
}
