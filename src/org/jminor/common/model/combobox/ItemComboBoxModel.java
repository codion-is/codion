/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Item;
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
public class ItemComboBoxModel<T> extends DefaultComboBoxModel {

  /** Constructs a new ItemComboBoxModel. */
  public ItemComboBoxModel() {
    initializeItems(null);
  }

  /**
   * Constructs a new ItemComboBoxModel
   * @param items the items
   */
  public ItemComboBoxModel(final Item<T>... items) {
    this(Arrays.asList(items));
  }

  public ItemComboBoxModel(final List<Item<T>> items) {
    initializeItems(new ArrayList<Item<T>>(items));
  }

  @Override
  public void setSelectedItem(final Object anObject) {
    if (!(anObject instanceof Item)) {
      super.setSelectedItem(getElementAt(indexOf(anObject)));
    }
    else {
      super.setSelectedItem(anObject);
    }
  }

  public int getIndexOfItem(final Object item) {
    return indexOf(item);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public Item<T> getSelectedItem() {
    return (Item<T>) super.getSelectedItem();
  }

  protected void initializeItems(final List<Item<T>> items) {
    if (items != null) {
      Collections.sort(items, new Comparator<Item<T>>() {
        /* Null items at front of list*/
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

      for (final Item item : items) {
        super.addElement(item);
      }
    }
  }

  private int indexOf(final Object item) {
    final int size = getSize();
    for (int i = 0; i < size; i++) {
      if (Util.equal(((Item)getElementAt(i)).getItem(), item)) {
        return i;
      }
    }

    return -1;
  }

  /**
   * An IconItem to use in a ItemComboBoxModel.
   */
  public static class IconItem<T> extends Item<T> implements Icon {

    private static final long serialVersionUID = 1;

    private final ImageIcon icon;

    public IconItem(final T item, final ImageIcon icon) {
      super(item, item == null ? "" : item.toString());
      Util.rejectNullValue(icon, "icon");
      this.icon = icon;
    }

    public int getIconHeight() {
      return icon.getIconHeight();
    }

    public int getIconWidth() {
      return icon.getIconWidth();
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
      icon.paintIcon(c, g, x, y);
    }

    @Override
    public String toString() {
      return getItem().toString();
    }
  }
}
