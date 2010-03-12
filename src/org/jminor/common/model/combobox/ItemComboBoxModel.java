/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Component;
import java.awt.Graphics;

public class ItemComboBoxModel extends DefaultComboBoxModel {

  public final Event evtSelectedItemChanged = new Event();

  /** Constructs a new ItemComboBoxModel. */
  public ItemComboBoxModel() {
    initializeItems(null);
  }

  /**
   * Constructs a new ItemComboBoxModel
   * @param items the items
   */
  public ItemComboBoxModel(final Item... items) {
    initializeItems(items);
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedItem(final Object item) {
    if (!(item instanceof Item))
      super.setSelectedItem(getElementAt(indexOf(item)));
    else
      super.setSelectedItem(item);

    evtSelectedItemChanged.fire();
  }

  public int getIndexOfItem(final Object item) {
    return indexOf(item);
  }

  protected void initializeItems(final Item[] items) {
    if (items != null) {
      for (final Item item : items)
        super.addElement(item);
    }
  }

  private int indexOf(final Object item) {
    final int size = getSize();
    for (int i = 0; i < size; i++) {
      if (Util.equal(((Item)getElementAt(i)).getItem(), item))
        return i;
    }

    return -1;
  }

  public static class Item<T> {

    private final T item;
    private final String caption;

    public Item(final T item, final String caption) {
      this.item = item;
      this.caption = caption;
    }

    /**
     * @return the caption
     */
    public String getCaption() {
      return caption;
    }

    public T getItem() {
      return item;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return getCaption();
    }
  }

  public static class IconItem extends Item<Object> implements Icon {

    private final ImageIcon icon;

    public IconItem(final Object item, final ImageIcon icon) {
      super(item, null);
      if (icon == null)
        throw new IllegalArgumentException("ItemComboBoxModel.IconItem must have an icon");
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
    public void paintIcon(Component c, Graphics g, int x, int y) {
      icon.paintIcon(c, g, x, y);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return getItem().toString();
    }
  }
}
