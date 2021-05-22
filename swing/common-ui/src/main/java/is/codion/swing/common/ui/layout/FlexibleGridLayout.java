/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.layout;

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

  private final boolean fixedRowHeights;
  private final boolean fixedColumnWidths;

  private int fixedColumnWidth;
  private int fixedRowHeight;

  private FlexibleGridLayout(final int rows, final int cols, final int hgap, final int vgap,
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

  /**
   * @return a builder for {@link FlexibleGridLayout}.
   */
  public static Builder builder() {
    return new DefaultBuilder();
  }

  /**
   * A builder for {@link FlexibleGridLayout}.
   */
  public interface Builder {

    /**
     * @param rows the number of rows
     * @return this builder instance
     */
    Builder rows(int rows);

    /**
     *
     * @param columns the number of columns
     * @return this builder instance
     */
    Builder columns(int columns);

    /**
     * @param horizontalGap the horizontal gap
     * @return this builder instance
     */
    Builder horizontalGap(int horizontalGap);

    /**
     * @param verticalGap the vertical gap
     * @return this builder instance
     */
    Builder verticalGap(int verticalGap);

    /**
     * @param fixRowHeights true if rows should have a
     * fixed height according to the tallest component
     * @return this builder instance
     */
    Builder fixRowHeights(boolean fixRowHeights);

    /**
     * @param fixColumnWidths true if columns should have a
     * fixed width according to the widest component
     * @return this builder instance
     */
    Builder fixColumnWidths(boolean fixColumnWidths);

    /**
     * @param fixedRowHeight the fixed row height
     * @return this builder instance
     */
    Builder fixedRowHeight(int fixedRowHeight);

    /**
     * @param fixedColumnWidth the fixed column width
     * @return this builder instance
     */
    Builder fixedColumnWidth(int fixedColumnWidth);

    /**
     * @return a new layout instance
     */
    FlexibleGridLayout build();
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

  private static final class DefaultBuilder implements Builder {

    private int rows = 0;
    private int columns = 0;
    private int horizontalGap = 0;
    private int verticalGap = 0;
    private boolean fixRowHeights = false;
    private boolean fixColumnWidths = false;
    private int fixedRowHeight;
    private int fixedColumnWidth;

    @Override
    public Builder rows(final int rows) {
      this.rows = rows;
      return this;
    }

    @Override
    public Builder columns(final int columns) {
      this.columns = columns;
      return this;
    }

    @Override
    public Builder horizontalGap(final int horizontalGap) {
      this.horizontalGap = horizontalGap;
      return this;
    }

    @Override
    public Builder verticalGap(final int verticalGap) {
      this.verticalGap = verticalGap;
      return this;
    }

    @Override
    public Builder fixRowHeights(final boolean fixRowHeights) {
      this.fixRowHeights = fixRowHeights;
      return this;
    }

    @Override
    public Builder fixColumnWidths(final boolean fixColumnWidths) {
      this.fixColumnWidths = fixColumnWidths;
      return this;
    }

    @Override
    public Builder fixedRowHeight(final int fixedRowHeight) {
      this.fixedRowHeight = fixedRowHeight;
      return this;
    }

    @Override
    public Builder fixedColumnWidth(final int fixedColumnWidth) {
      this.fixedColumnWidth = fixedColumnWidth;
      return this;
    }

    @Override
    public FlexibleGridLayout build() {
      final FlexibleGridLayout layout = new FlexibleGridLayout(rows, columns, horizontalGap, verticalGap, fixRowHeights, fixColumnWidths);
      if (fixedRowHeight > 0) {
        layout.setFixedRowHeight(fixedRowHeight);
      }
      if (fixedColumnWidth > 0) {
        layout.setFixedColumnWidth(fixedColumnWidth);
      }

      return layout;
    }
  }
}