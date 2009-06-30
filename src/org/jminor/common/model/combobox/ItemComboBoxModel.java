/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
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
    initItems(null);
  }

  /**
   * Constructs a new ItemComboBoxModel
   * @param items the items
   */
  public ItemComboBoxModel(final IItem... items) {
    initItems(items);
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedItem(final Object item) {
    if (!(item instanceof IItem))
      super.setSelectedItem(getElementAt(indexOf(item)));
    else
      super.setSelectedItem(item);

    evtSelectedItemChanged.fire();
  }

  public int getIndexOfItem(final Object item) {
    return indexOf(item);
  }

  protected void initItems(final IItem[] items) {
    if (items != null) {
      for (final IItem item : items)
        super.addElement(item);
    }
  }

  private int indexOf(final Object item) {
    final int size = getSize();
    for (int i = 0; i < size; i++) {
      if (Util.equal(((IItem)getElementAt(i)).getItem(), item))
        return i;
    }

    return -1;
  }

  public interface IItem<T> {
    /**
     * @return the actual item
     */
    public T getItem();

    /** {@inheritDoc} */
    @Override
    public String toString();
  }

  public static class Item<T> implements IItem<T> {

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

    /** {@inheritDoc} */
    public T getItem() {
      return item;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return getCaption();
    }
  }

  public static class IconItem implements Icon, IItem<Object> {

    private final Object item;
    private final ImageIcon icon;

    public IconItem(final Object item, final ImageIcon icon) {
      if (icon == null)
        throw new IllegalArgumentException("ItemComboBoxModel.IconItem must have an icon");

      this.item = item;
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
    public Object getItem() {
      return item;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return item.toString();
    }
  }
}
