/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Item;
import org.jminor.common.model.Util;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Collection;
import java.util.Comparator;

/**
 * A ComboBoxModel implementation based on the {@link Item} class.
 */
public class ItemComboBoxModel<T> extends DefaultFilteredComboBoxModel<Item<T>> {

  /** Constructs a new ItemComboBoxModel. */
  public ItemComboBoxModel() {}

  /**
   * Constructs a new ItemComboBoxModel
   * @param items the items
   */
  public ItemComboBoxModel(final Collection<? extends Item<T>> items) {
    setContents(items);
  }

  /**
   * Constructs a new ItemComboBoxModel
   * @param sortComparator the Comparator used to sort the contents of this combo box model,
   * if null then the original item order will be preserved
   * @param items the items
   */
  public ItemComboBoxModel(final Comparator<? super Item<T>> sortComparator, final Collection<? extends Item<T>> items) {
    super(null, sortComparator);
    setContents(items);
  }

  /**
   * @param item the item
   * @return the index of the given item
   */
  public final int getIndexOfItem(final T item) {
    return indexOf(item);
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

  private int indexOf(final T item) {
    final int size = getSize();
    for (int i = 0; i < size; i++) {
      if (Util.equal(((Item) getElementAt(i)).getItem(), item)) {
        return i;
      }
    }

    return -1;
  }

  /**
   * An IconItem to use in a ItemComboBoxModel.
   */
  public static final class IconItem<T> extends Item<T> implements Icon {

    private final ImageIcon icon;

    /**
     * Constructs a new IconItem.
     * @param item the item this IconItem represents
     * @param icon the icon
     */
    public IconItem(final T item, final ImageIcon icon) {
      super(item, "");
      Util.rejectNullValue(icon, "icon");
      this.icon = icon;
    }

    /** {@inheritDoc} */
    @Override
    public int getIconHeight() {
      return icon.getIconHeight();
    }

    /** {@inheritDoc} */
    @Override
    public int getIconWidth() {
      return icon.getIconWidth();
    }

    /** {@inheritDoc} */
    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      icon.paintIcon(c, g, x, y);
    }
  }
}
