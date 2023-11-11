/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.layout;

import is.codion.common.Configuration;
import is.codion.common.value.Value;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * A utility class for layouts. All layouts created use the centrally configured hgap and vgap.
 * @see #GAP
 */
public final class Layouts {

  private static final int DEFAULT_GAP = 5;

  /**
   * Specifies the default horizontal and vertical component gap, used by the layout factory methods, by default this is 5
   * @see #borderLayout()
   * @see #flowLayout(int)
   * @see #gridLayout(int, int)
   * @see #flexibleGridLayout(int, int)
   */
  public static final Value<Integer> GAP = Configuration.integerValue("is.codion.swing.common.ui.layout.Layouts.gap", DEFAULT_GAP);

  private Layouts() {}

  /**
   * Creates a BorderLayout using the default vertical and horizontal gap value
   * @return a BorderLayout
   * @see #GAP
   */
  public static BorderLayout borderLayout() {
    return new BorderLayout(GAP.get(), GAP.get());
  }

  /**
   * Creates a FlowLayout using the default vertical and horizontal gap value
   * @param alignment the alignment
   * @return a FlowLayout
   * @see #GAP
   */
  public static FlowLayout flowLayout(int alignment) {
    return new FlowLayout(alignment, GAP.get(), GAP.get());
  }

  /**
   * Creates a GridLayout using the default vertical and horizontal gap value
   * @param rows the number of rows
   * @param columns the number of columns
   * @return a GridLayout
   * @see #GAP
   */
  public static GridLayout gridLayout(int rows, int columns) {
    return new GridLayout(rows, columns, GAP.get(), GAP.get());
  }

  /**
   * Creates a FlexibleGridLayout using the default vertical and horizontal gap value,
   * with neither row heights nor column widths fixed.
   * @param rows the number of rows
   * @param columns the number of columns
   * @return a FlexibleGridLayout
   * @see #GAP
   */
  public static FlexibleGridLayout flexibleGridLayout(int rows, int columns) {
    return flexibleGridLayout()
            .rowsColumns(rows, columns)
            .build();
  }

  /**
   * Creates a FlexibleGridLayout.Builder using the default vertical and horizontal gap value
   * @return a FlexibleGridLayout.Builder instance
   * @see #GAP
   */
  public static FlexibleGridLayout.Builder flexibleGridLayout() {
    return FlexibleGridLayout.builder()
            .horizontalGap(GAP.get())
            .verticalGap(GAP.get());
  }
}
