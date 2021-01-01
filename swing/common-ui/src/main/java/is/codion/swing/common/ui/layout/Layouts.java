/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.layout;

import is.codion.common.Configuration;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixColumnWidths;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixRowHeights;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * A utility class for layouts. All layouts created use the centrally configured hgap and vgap.
 * @see #HORIZONTAL_VERTICAL_GAP
 */
public final class Layouts {

  private static final int DEFAULT_HOR_VERT_GAP = 5;

  /**
   * Specifies the default horizontal and vertical component gap, used by the layout factory methods, by default this is 5
   * @see #borderLayout()
   * @see #flowLayout(int)
   * @see #gridLayout(int, int)
   * @see #flexibleGridLayout(int, int)
   */
  public static final Value<Integer> HORIZONTAL_VERTICAL_GAP =
          Configuration.integerValue("codion.swing.ui.horizontalVerticalGap", DEFAULT_HOR_VERT_GAP);

  private Layouts() {}

  /**
   * Creates a BorderLayout using the default vertical and horizontal gap value
   * @return a BorderLayout
   * @see #HORIZONTAL_VERTICAL_GAP
   */
  public static BorderLayout borderLayout() {
    return new BorderLayout(HORIZONTAL_VERTICAL_GAP.get(), HORIZONTAL_VERTICAL_GAP.get());
  }

  /**
   * Creates a FlowLayout using the default vertical and horizontal gap value
   * @param alignment the alignment
   * @return a FlowLayout
   * @see #HORIZONTAL_VERTICAL_GAP
   */
  public static FlowLayout flowLayout(final int alignment) {
    return new FlowLayout(alignment, HORIZONTAL_VERTICAL_GAP.get(), HORIZONTAL_VERTICAL_GAP.get());
  }

  /**
   * Creates a GridLayout using the default vertical and horizontal gap value
   * @param rows the number of rows
   * @param columns the number of columns
   * @return a GridLayout
   * @see #HORIZONTAL_VERTICAL_GAP
   */
  public static GridLayout gridLayout(final int rows, final int columns) {
    return new GridLayout(rows, columns, HORIZONTAL_VERTICAL_GAP.get(), HORIZONTAL_VERTICAL_GAP.get());
  }

  /**
   * Creates a FlexibleGridLayout using the default vertical and horizontal gap value,
   * with neither row heights nor column widths fixed.
   * @param rows the number of rows
   * @param columns the number of columns
   * @return a FlexibleGridLayout
   * @see #HORIZONTAL_VERTICAL_GAP
   */
  public static FlexibleGridLayout flexibleGridLayout(final int rows, final int columns) {
    return flexibleGridLayout(rows, columns, FixRowHeights.NO, FixColumnWidths.NO);
  }

  /**
   * Creates a FlexibleGridLayout using the default vertical and horizontal gap value
   * @param rows the number of rows
   * @param columns the number of columns
   * @param fixRowHeights if yes then the height of the rows is fixed as the largest value
   * @param fixColumnWidths if yes then the width of the columns is fixed as the largest value
   * @return a FlexibleGridLayout
   * @see #HORIZONTAL_VERTICAL_GAP
   */
  public static FlexibleGridLayout flexibleGridLayout(final int rows, final int columns,
                                                      final FixRowHeights fixRowHeights,
                                                      final FixColumnWidths fixColumnWidths) {
    return new FlexibleGridLayout(rows, columns, HORIZONTAL_VERTICAL_GAP.get(),
            HORIZONTAL_VERTICAL_GAP.get(), fixRowHeights, fixColumnWidths);
  }
}
