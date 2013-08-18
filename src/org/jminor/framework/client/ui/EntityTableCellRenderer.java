/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import javax.swing.table.TableCellRenderer;

/**
 * A TableCellRenderer with the added options of visually displaying if a
 * cell (or column) is involved in a search criteria and showing its
 * contents in a tooltip
 */
public interface EntityTableCellRenderer extends TableCellRenderer {

  /**
   * @return true if the search state should be represented visually
   */
  boolean isIndicateSearch();

  /**
   * If true then columns being search by have different background color
   * @param indicateSearch the value
   */
  void setIndicateSearch(final boolean indicateSearch);

  /**
   * @return if true then the cell data is added as a tool tip for the cell
   */
  boolean isTooltipData();

  /**
   * @param tooltipData if true then the cell data is added as a tool tip for the cell
   */
  void setTooltipData(final boolean tooltipData);
}
