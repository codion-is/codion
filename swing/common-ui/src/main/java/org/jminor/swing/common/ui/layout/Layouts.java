/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.layout;

import org.jminor.common.Configuration;
import org.jminor.common.value.Value;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public final class Layouts {

  private static final int DEFAULT_HOR_VERT_GAP = 5;

  /**
   * Specifies the default horizontal and vertical component gap, used by the layout factory methods, by default this is 5
   * @see #createBorderLayout()
   * @see #createFlowLayout(int)
   * @see #createGridLayout(int, int)
   * @see #createFlexibleGridLayout(int, int, boolean, boolean)
   */
  public static final Value<Integer> HORIZONTAL_VERTICAL_GAP =
          Configuration.integerValue("jminor.ui.horizontalVerticalGap", DEFAULT_HOR_VERT_GAP);

  private Layouts() {}

  /**
   * Creates a BorderLayout using the default vertical and horizontal gap value
   * @return a BorderLayout
   * @see #HORIZONTAL_VERTICAL_GAP
   */
  public static BorderLayout createBorderLayout() {
    return new BorderLayout(HORIZONTAL_VERTICAL_GAP.get(), HORIZONTAL_VERTICAL_GAP.get());
  }

  /**
   * Creates a FlowLayout using the default vertical and horizontal gap value
   * @param alignment the alignment
   * @return a FlowLayout
   * @see #HORIZONTAL_VERTICAL_GAP
   */
  public static FlowLayout createFlowLayout(final int alignment) {
    return new FlowLayout(alignment, HORIZONTAL_VERTICAL_GAP.get(), HORIZONTAL_VERTICAL_GAP.get());
  }

  /**
   * Creates a GridLayout using the default vertical and horizontal gap value
   * @param rows the number of rows
   * @param columns the number of columns
   * @return a GridLayout
   * @see #HORIZONTAL_VERTICAL_GAP
   */
  public static GridLayout createGridLayout(final int rows, final int columns) {
    return new GridLayout(rows, columns, HORIZONTAL_VERTICAL_GAP.get(), HORIZONTAL_VERTICAL_GAP.get());
  }

  /**
   * Creates a FlexibleGridLayout using the default vertical and horizontal gap value
   * @param rows the number of rows
   * @param columns the number of columns
   * @param fixRowHeights if true then the height of the rows is fixed as the largest value
   * @param fixColumnWidths if true then the width of the columns is fixed as the largest value
   * @return a FlexibleGridLayout
   * @see #HORIZONTAL_VERTICAL_GAP
   */
  public static FlexibleGridLayout createFlexibleGridLayout(final int rows, final int columns,
                                                            final boolean fixRowHeights, final boolean fixColumnWidths) {
    return new FlexibleGridLayout(rows, columns, HORIZONTAL_VERTICAL_GAP.get(),
            HORIZONTAL_VERTICAL_GAP.get(), fixRowHeights, fixColumnWidths);
  }
}
