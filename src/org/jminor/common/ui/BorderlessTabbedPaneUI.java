/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 *
 */
package org.jminor.common.ui;

import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Graphics;
import java.awt.Insets;

public class BorderlessTabbedPaneUI extends BasicTabbedPaneUI {

  /** {@inheritDoc} */
  protected Insets getContentBorderInsets(final int tabPlacement) {
    return new Insets(0,0,0,0);
  }

  /** {@inheritDoc} */
  protected void paintContentBorder(final Graphics graphics, final int tabPlacement,
                                    final int selectedIndex) {}
}
