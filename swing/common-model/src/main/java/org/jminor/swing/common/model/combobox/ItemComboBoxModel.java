/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.combobox;

import org.jminor.common.Item;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/**
 * A ComboBoxModel implementation based on the {@link Item} class.
 * @param <T> the type of value wrapped by this combo box models items
 */
public class ItemComboBoxModel<T> extends SwingFilteredComboBoxModel<Item<T>> {

  /** Constructs a new ItemComboBoxModel. */
  public ItemComboBoxModel() {}

  /**
   * Constructs a new ItemComboBoxModel
   * @param items the items
   */
  public ItemComboBoxModel(final Collection<Item<T>> items) {
    setContents(items);
  }

  /**
   * Constructs a new ItemComboBoxModel
   * @param sortComparator the Comparator used to sort the contents of this combo box model,
   * if null then the original item order will be preserved
   * @param items the items
   */
  public ItemComboBoxModel(final Comparator<Item<T>> sortComparator, final Collection<Item<T>> items) {
    super(null, sortComparator);
    setContents(items);
  }

  /**
   * @param item the item
   * @return the index of the given item
   */
  public final int indexOf(final T item) {
    for (int i = 0; i < getSize(); i++) {
      if (Objects.equals(getElementAt(i).getItem(), item)) {
        return i;
      }
    }

    return -1;
  }

  @Override
  protected final Item<T> translateSelectionItem(final Object item) {
    if (item instanceof Item) {
      return (Item<T>) item;
    }

    final int index = indexOf((T) item);
    if (index >= 0) {
      return getElementAt(index);
    }

    return null;
  }
}
