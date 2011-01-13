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
import java.util.Arrays;
import java.util.List;

/**
 * A ComboBoxModel implementation based on the <code>ItemComboBoxModel.Item</code> class.
 */
public class ItemComboBoxModel<T> extends DefaultFilteredComboBoxModel<Item<T>> {

  /** Constructs a new ItemComboBoxModel. */
  public ItemComboBoxModel() {}

  /**
   * Constructs a new ItemComboBoxModel
   * @param items the items
   */
  public ItemComboBoxModel(final Item<T>... items) {
    this(Arrays.asList(items));
  }

  /**
   * Constructs a new ItemComboBoxModel
   * @param items the items
   */
  public ItemComboBoxModel(final List<Item<T>> items) {
    setContents(items);
  }

  /**
   * @param item the item
   * @return the index of the given item
   */
  public final int getIndexOfItem(final Object item) {
    return indexOf(item);
  }

  @Override
  protected final Object translateSelectionItem(final Object item) {
    if (item instanceof Item) {
      return item;
    }

    final int index = indexOf(item);
    if (index >= 0) {
      return getElementAt(index);
    }

    return null;
  }

  private int indexOf(final Object item) {
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

    private static final long serialVersionUID = 1;

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
    public int getIconHeight() {
      return icon.getIconHeight();
    }

    /** {@inheritDoc} */
    public int getIconWidth() {
      return icon.getIconWidth();
    }

    /** {@inheritDoc} */
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      icon.paintIcon(c, g, x, y);
    }
  }
}
