/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;

/**
 * Grid Layout which allows components of different sizes.
 * @author unknown
 */
public final class FlexibleGridLayout extends GridLayout {

  private boolean fixedRowHeights = false;
  private boolean fixedColumnWidths = false;
  private int fixedColumnWidth;
  private int fixedRowHeight;

  public FlexibleGridLayout(final int rows, final int cols) {
    this(rows, cols, 0, 0, false, false);
  }

  public FlexibleGridLayout(final int rows, final int cols, final int hgap,final  int vgap) {
    this(rows, cols, hgap, vgap, false, false);
  }

  public FlexibleGridLayout(final int rows, final int cols, final int hgap, final int vgap,
                            final boolean fixRowHeights, final boolean fixColumnWidths) {
    super(rows, cols, hgap, vgap);
    this.fixedRowHeights = fixRowHeights;
    this.fixedColumnWidths = fixColumnWidths;
  }

  /**
   * @param height the fixed row height to use in this layout
   * @return this layout instance
   */
  public FlexibleGridLayout setFixedRowHeight(final int height) {
    fixedRowHeight = height;
    return this;
  }

  /**
   * @param width the fixed column width to use in this layout
   * @return this layout instance
   */
  public FlexibleGridLayout setFixedColumnWidth(final int width) {
    fixedColumnWidth = width;
    return this;
  }

  @Override
  public Dimension preferredLayoutSize(final Container parent) {
    synchronized (parent.getTreeLock()) {
      final Insets insets = parent.getInsets();
      final int numberOfComponents = parent.getComponentCount();
      int numberOfRows = getRows();
      int numberOfColumns = getColumns();
      if (numberOfRows > 0) {
        numberOfColumns = (numberOfComponents + numberOfRows - 1) / numberOfRows;
      }
      else {
        numberOfRows = (numberOfComponents + numberOfColumns - 1) / numberOfColumns;
      }
      final int[] columnWidths = new int[numberOfColumns];
      final int[] rowHeights = new int[numberOfRows];
      for (int i = 0; i < numberOfComponents; i++) {
        final int row = i / numberOfColumns;
        final int column = i % numberOfColumns;
        final Component comp = parent.getComponent(i);
        final Dimension d = comp.getPreferredSize();
        if (columnWidths[column] < d.width) {
          columnWidths[column] = d.width;
        }
        if (rowHeights[row] < d.height) {
          rowHeights[row] = d.height;
        }
      }
      //
      arrangeFixedSizes(columnWidths, rowHeights);
      //
      int newWidth = 0;
      for (int j = 0; j < numberOfColumns; j++) {
        newWidth += columnWidths[j];
      }
      int newHeight = 0;
      for (int i = 0; i < numberOfRows; i++) {
        newHeight += rowHeights[i];
      }
      return new Dimension(insets.left + insets.right + newWidth + (numberOfColumns-1)*getHgap(),
              insets.top + insets.bottom + newHeight + (numberOfRows-1)*getVgap());
    }
  }

  @Override
  public Dimension minimumLayoutSize(final Container parent) {
    synchronized (parent.getTreeLock()) {
      final Insets insets = parent.getInsets();
      final int numberOfComponents = parent.getComponentCount();
      int numberOfRows = getRows();
      int numberOfColumns = getColumns();
      if (numberOfRows > 0) {
        numberOfColumns = (numberOfComponents + numberOfRows - 1) / numberOfRows;
      }
      else {
        numberOfRows = (numberOfComponents + numberOfColumns - 1) / numberOfColumns;
      }
      final int[] columnWidths = new int[numberOfColumns];
      final int[] rowHeights = new int[numberOfRows];
      for (int i = 0; i < numberOfComponents; i++) {
        final int row = i / numberOfColumns;
        final int column = i % numberOfColumns;
        final Component comp = parent.getComponent(i);
        final Dimension d = comp.getMinimumSize();
        if (columnWidths[column] < d.width) {
          columnWidths[column] = d.width;
        }
        if (rowHeights[row] < d.height) {
          rowHeights[row] = d.height;
        }
      }
      //
      arrangeFixedSizes(columnWidths, rowHeights);
      //
      int newWidth = 0;
      for (int j = 0; j < numberOfColumns; j++) {
        newWidth += columnWidths[j];
      }
      int newHeight = 0;
      for (int i = 0; i < numberOfRows; i++) {
        newHeight += rowHeights[i];
      }
      return new Dimension(insets.left + insets.right + newWidth + (numberOfColumns-1)*getHgap(),
              insets.top + insets.bottom + newHeight + (numberOfRows-1)*getVgap());
    }
  }

  @Override
  public void layoutContainer(final Container parent) {
    synchronized (parent.getTreeLock()) {
      final Insets insets = parent.getInsets();
      final int numberOfComponents = parent.getComponentCount();
      int numberOfRows = getRows();
      int numberOfColumns = getColumns();
      if (numberOfComponents == 0) {
        return;
      }
      if (numberOfRows > 0) {
        numberOfColumns = (numberOfComponents + numberOfRows - 1) / numberOfRows;
      }
      else {
        numberOfRows = (numberOfComponents + numberOfColumns - 1) / numberOfColumns;
      }
      final int horizontalGap = getHgap();
      final int verticalGap = getVgap();
      // scaling factors
      final Dimension pd = preferredLayoutSize(parent);
      final double sw = (1.0 * parent.getWidth()) / pd.width;
      final double sh = (1.0 * parent.getHeight()) / pd.height;
      // scale
      final int[] columnWidths = new int[numberOfColumns];
      final int[] rowHeights = new int[numberOfRows];
      for (int i = 0; i < numberOfComponents; i++) {
        final int row = i / numberOfColumns;
        final int column = i % numberOfColumns;
        final Component currentComponent = parent.getComponent(i);
        final Dimension currCompPrefSize = currentComponent.getPreferredSize();
        currCompPrefSize.width = (int) (sw * currCompPrefSize.width);
        currCompPrefSize.height = (int) (sh * currCompPrefSize.height);
        if (columnWidths[column] < currCompPrefSize.width) {
          columnWidths[column] = currCompPrefSize.width;
        }
        if (rowHeights[row] < currCompPrefSize.height) {
          rowHeights[row] = currCompPrefSize.height;
        }
      }
      //
      arrangeFixedSizes(columnWidths, rowHeights);
      //
      for (int c = 0, x = insets.left; c < numberOfColumns; c++) {
        for (int r = 0, y = insets.top; r < numberOfRows; r++) {
          final int i = r * numberOfColumns + c;
          if (i < numberOfComponents) {
            parent.getComponent(i).setBounds(x, y, columnWidths[c], rowHeights[r]);
          }
          y += rowHeights[r] + verticalGap;
        }
        x += columnWidths[c] + horizontalGap;
      }
    }
  }

  private void arrangeFixedSizes(final int[] columnWidths, final int[] rowHeights) {
    if (fixedColumnWidths) {
      int maxColumnWidth = 0;
      if (fixedColumnWidth <= 0) {
        for (final int columnWidth : columnWidths) {
          maxColumnWidth = Math.max(columnWidth, maxColumnWidth);
        }
      }
      else {
        maxColumnWidth = fixedColumnWidth;
      }
      for (int i = 0; i < columnWidths.length; i++) {
        columnWidths[i] = maxColumnWidth;
      }
    }
    if (fixedRowHeights) {
      int maxRowHeight = 0;
      if (fixedRowHeight <= 0) {
        for (final int rowHeight : rowHeights) {
          maxRowHeight = Math.max(rowHeight, maxRowHeight);
        }
      }
      else {
        maxRowHeight = fixedRowHeight;
      }
      for (int i = 0; i < rowHeights.length; i++) {
        rowHeights[i] = maxRowHeight;
      }
    }
  }
}