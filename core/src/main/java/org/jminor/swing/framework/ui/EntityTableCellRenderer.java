/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import javax.swing.table.TableCellRenderer;

/**
 * A TableCellRenderer with the added options of visually displaying if a
 * cell (or column) is involved in a criteria and showing its contents in a tooltip
 */
public interface EntityTableCellRenderer extends TableCellRenderer {

  /**
   * @return true if the criteria state should be represented visually
   */
  boolean isIndicateCriteria();

  /**
   * If true then columns involved in a criteria have different background color
   * @param indicateSearch the value
   */
  void setIndicateCriteria(final boolean indicateSearch);

  /**
   * @return if true then the cell data is added as a tool tip for the cell
   */
  boolean isTooltipData();

  /**
   * @param tooltipData if true then the cell data is added as a tool tip for the cell
   */
  void setTooltipData(final boolean tooltipData);
}
