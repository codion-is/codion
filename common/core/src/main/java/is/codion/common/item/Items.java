/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.item;

/**
 * Factory class for {@link Item} instances.
 */
public final class Items {

  private static final Item NULL_ITEM = new DefaultItem(null, "");

  private Items() {}

  /**
   * Returns an Item, with the caption as item.toString() or an empty string in case of a null value
   * @param value the value, may be null
   * @param <T> the value type
   * @return an Item based on the given value
   */
  public static <T> Item<T> item(final T value) {
    if (value == null) {
      return NULL_ITEM;
    }

    return item(value, value.toString());
  }

  /**
   * Instantiates a new Item.
   * @param value the value, may be null
   * @param caption the caption
   * @param <T> the value type
   * @return an Item based on the given value and caption
   * @throws NullPointerException if caption is null
   */
  public static <T> Item<T> item(final T value, final String caption) {
    return new DefaultItem<>(value, caption);
  }
}
