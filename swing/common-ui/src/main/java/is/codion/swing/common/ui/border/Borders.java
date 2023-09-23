/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.border;

import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

/**
 * Utility class for working with Borders.
 */
public final class Borders {

  private Borders() {}

  /**
   * Creates en empty border using the value of {@link Layouts#HORIZONTAL_VERTICAL_GAP}
   * as the top, bottom, left and right values.
   * @return a new empty border
   */
  public static Border emptyBorder() {
    int gap = Layouts.HORIZONTAL_VERTICAL_GAP.get();

    return BorderFactory.createEmptyBorder(gap, gap, gap, gap);
  }
}
