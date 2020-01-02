/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;

/**
 * A TableCellRenderer with the added options of visually displaying if a
 * cell (or column) is involved in a condition and showing its contents in a tooltip
 */
public interface EntityTableCellRenderer extends TableCellRenderer {

  /**
   * @return true if the condition state should be represented visually
   */
  boolean isIndicateCondition();

  /**
   * If true then columns involved in a condition have different background color
   * @param indicateCondition the value
   */
  void setIndicateCondition(boolean indicateCondition);

  /**
   * @return if true then the cell data is added as a tool tip for the cell
   */
  boolean isTooltipData();

  /**
   * @param tooltipData if true then the cell data is added as a tool tip for the cell
   */
  void setTooltipData(boolean tooltipData);

  /**
   * Provides the foreground to use for cells in the given table.
   * @param table the table
   * @param selected true if the cell is selected
   * @return the foreground color
   */
  default Color getForeground(final JTable table, final boolean selected) {
    if (selected) {
      return table.getSelectionForeground();
    }

    return table.getForeground();
  }

  /**
   * Provides the background color for cells in the given table.
   * @param table the table
   * @param row the row
   * @param selected true if the cell is selected
   * @return the background color
   */
  default Color getBackground(final JTable table, final int row, final boolean selected) {
    if (selected) {
      return table.getSelectionBackground();
    }

    return table.getBackground();
  }
}
