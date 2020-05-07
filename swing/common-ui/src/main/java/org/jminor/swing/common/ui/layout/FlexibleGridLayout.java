/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Arrays;

/**
 * Grid Layout which allows components of different sizes.
 * @author unknown
 */
public final class FlexibleGridLayout extends GridLayout {

  private static final double ONE_POINT_O = 1.0;

  /**
   * Specifies whether row heights should be fixed.
   */
  public enum FixRowHeights {
    /**
     * Row heights should be fixed.
     */
    YES,
    /**
     * Row heights should not be fixed.
     */
    NO
  }

  /**
   * Specifies whether column widths should be fixed.
   */
  public enum FixColumnWidths {
    /**
     * Column widths should be fixed.
     */
    YES,
    /**
     * Column widths should not be fixed.
     */
    NO
  }

  private final boolean fixedRowHeights;
  private final boolean fixedColumnWidths;

  private int fixedColumnWidth;
  private int fixedRowHeight;

  /**
   * Instantiates a new FlexibleGridLayout
   * @param rows the number of rows
   * @param cols the number of columns
   */
  public FlexibleGridLayout(final int rows, final int cols) {
    this(rows, cols, 0, 0, FixRowHeights.NO, FixColumnWidths.NO);
  }

  /**
   * Instantiates a new FlexibleGridLayout
   * @param rows the number of rows
   * @param cols the number of columns
   * @param hgap the horizontal gap
   * @param vgap the vertical gap
   */
  public FlexibleGridLayout(final int rows, final int cols, final int hgap, final int vgap) {
    this(rows, cols, hgap, vgap, FixRowHeights.NO, FixColumnWidths.NO);
  }

  /**
   * Instantiates a new FlexibleGridLayout
   * @param rows the number of rows
   * @param cols the number of columns
   * @param hgap the horizontal gap
   * @param vgap the vertical gap
   * @param fixRowHeights if yes then the row heights are fixed according to the largest row
   * @param fixColumnWidths if yes then the column widths are fixed according to the largest column
   */
  public FlexibleGridLayout(final int rows, final int cols, final int hgap, final int vgap,
                            final FixRowHeights fixRowHeights, final FixColumnWidths fixColumnWidths) {
    super(rows, cols, hgap, vgap);
    this.fixedRowHeights = fixRowHeights == FixRowHeights.YES;
    this.fixedColumnWidths = fixColumnWidths == FixColumnWidths.YES;
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
    return layoutSize(parent, true);
  }

  @Override
  public Dimension minimumLayoutSize(final Container parent) {
    return layoutSize(parent, false);
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
      final double sw = (ONE_POINT_O * parent.getWidth()) / pd.getWidth();
      final double sh = (ONE_POINT_O * parent.getHeight()) / pd.getHeight();
      // scale
      final int[] columnWidths = new int[numberOfColumns];
      final int[] rowHeights = new int[numberOfRows];
      for (int i = 0; i < numberOfComponents; i++) {
        final int row = i / numberOfColumns;
        final int column = i % numberOfColumns;
        final Component currentComponent = parent.getComponent(i);
        final Dimension currCompPrefSize = currentComponent.getPreferredSize();
        currCompPrefSize.width = (int) (sw * currCompPrefSize.getWidth());
        currCompPrefSize.height = (int) (sh * currCompPrefSize.getHeight());
        if (columnWidths[column] < currCompPrefSize.getWidth()) {
          columnWidths[column] = (int) currCompPrefSize.getWidth();
        }
        if (rowHeights[row] < currCompPrefSize.getHeight()) {
          rowHeights[row] = (int) currCompPrefSize.getHeight();
        }
      }

      arrangeFixedSizes(columnWidths, rowHeights);

      int x = insets.left;
      for (int c = 0; c < numberOfColumns; c++) {
        int y = insets.top;
        for (int r = 0; r < numberOfRows; r++) {
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
      Arrays.fill(columnWidths, maxColumnWidth);
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
      Arrays.fill(rowHeights, maxRowHeight);
    }
  }

  private Dimension layoutSize(final Container parent, final boolean preferredSize) {
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
        final Dimension d = preferredSize ? comp.getPreferredSize() : comp.getMinimumSize();
        if (columnWidths[column] < d.getWidth()) {
          columnWidths[column] = (int) d.getWidth();
        }
        if (rowHeights[row] < d.getHeight()) {
          rowHeights[row] = (int) d.getHeight();
        }
      }

      arrangeFixedSizes(columnWidths, rowHeights);

      final int newWidth = Arrays.stream(columnWidths).sum();
      final int newHeight = Arrays.stream(rowHeights).sum();

      return new Dimension(insets.left + insets.right + newWidth + (numberOfColumns - 1) * getHgap(),
              insets.top + insets.bottom + newHeight + (numberOfRows - 1) * getVgap());
    }
  }
}