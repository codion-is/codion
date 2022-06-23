/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.item;

import java.io.Serializable;

/**
 * A class encapsulating a constant value and a caption representing the value.
 * Comparing Items is based on their caption.
 * Factory class for {@link Item} instances.
 * @param <T> the type of the value
 * @see Item#item(Object)
 * @see Item#item(Object, String)
 * @see Item#itemI18n(Object, String, String)
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

  /**
   * Returns an Item, with the caption as item.toString() or an empty string in case of a null value
   * @param value the value, may be null
   * @param <T> the value type
   * @return an Item based on the given value
   */
  static <T> Item<T> item(T value) {
    if (value == null) {
      return (Item<T>) DefaultItem.NULL_ITEM;
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
  static <T> Item<T> item(T value, String caption) {
    return new DefaultItem<>(value, caption);
  }

  /**
   * Instantiates a new Item, which gets its caption from a resource bundle.
   * Note that the caption is cached, so that changing the {@link java.util.Locale} after the
   * first time {@link Item#getCaption} is called will not change the caption.
   * @param value the value, may be null
   * @param resourceBundleName the resource bundle name
   * @param resourceBundleKey the resource bundle key for the item caption
   * @param <T> the value type
   * @return an Item based on the given value and resource bundle
   */
  static <T> Item<T> itemI18n(T value, String resourceBundleName, String resourceBundleKey) {
    return new ItemI18n<>(value, resourceBundleName, resourceBundleKey);
  }
}
