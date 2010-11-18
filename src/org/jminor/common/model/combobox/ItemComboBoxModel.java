/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Item;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.Util;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A ComboBoxModel implementation based on the <code>ItemComboBoxModel.Item</code> class.
 */
public class ItemComboBoxModel<T> extends DefaultComboBoxModel implements Refreshable {

  /** Constructs a new ItemComboBoxModel. */
  public ItemComboBoxModel() {
    setItems(null);
  }

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
    setItems(items);
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedItem(final Object anObject) {
    if (!(anObject instanceof Item)) {
      super.setSelectedItem(getElementAt(indexOf(anObject)));
    }
    else {
      super.setSelectedItem(anObject);
    }
  }

  /**
   * @param item the item
   * @return the index of the given item
   */
  public final int getIndexOfItem(final Object item) {
    return indexOf(item);
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  @Override
  public final Item<T> getSelectedItem() {
    return (Item<T>) super.getSelectedItem();
  }

  /** {@inheritDoc} */
  public final void clear() {
    removeAllElements();
  }

  /**
   * Refreshes the data in this combo box model, this default implementation
   * does nothing, override to provide dynamic data.
   * @see #setItems(java.util.List)
   */
  public void refresh() {}

  /**
   * Sorts the given list and adds the items to this combo box model.
   * @param items the items to show in this combo box model
   */
  protected final void setItems(final List<Item<T>> items) {
    clear();
    if (items == null) {
      return;
    }

    final List<Item<T>> itemsToAdd = new ArrayList<Item<T>>(items);
    Collections.sort(itemsToAdd, new Comparator<Item<T>>() {
      /** Null items at front of list*/
      public int compare(final Item<T> o1, final Item<T> o2) {
        if (o1.getItem() == null && o2.getItem() == null) {
          return o1.compareTo(o2);
        }
        if (o1.getItem() == null) {
          return -1;
        }
        if (o2.getItem() == null) {
          return 1;
        }

        return o1.compareTo(o2);
      }
    });

    for (final Item item : itemsToAdd) {
      super.addElement(item);
    }
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
      super(item, item == null ? "" : item.toString());
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
