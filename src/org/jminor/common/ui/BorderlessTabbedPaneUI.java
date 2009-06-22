/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Insets;

public class BorderlessTabbedPaneUI extends BasicTabbedPaneUI {

  /** {@inheritDoc} */
  @Override
  protected Insets getContentBorderInsets(final int tabPlacement) {
    return new Insets(2,0,0,0);
  }
}
