/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import javax.swing.table.TableCellRenderer;

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
   * @param indicateSearch the value
   */
  void setIndicateCondition(final boolean indicateSearch);

  /**
   * @return if true then the cell data is added as a tool tip for the cell
   */
  boolean isTooltipData();

  /**
   * @param tooltipData if true then the cell data is added as a tool tip for the cell
   */
  void setTooltipData(final boolean tooltipData);
}
