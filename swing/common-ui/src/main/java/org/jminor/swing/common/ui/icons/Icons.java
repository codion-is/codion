/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.icons;

import javax.swing.ImageIcon;

/**
 * Provides icons for common ui components.
 */
public interface Icons {

  /**
   * @return icon for the 'filter' action.
   */
  ImageIcon filter();

  /**
   * @return icon for the 'configure' action.
   */
  ImageIcon configure();

  /**
   * @return icon for the jminor logo
   */
  ImageIcon logo();

  /**
   * @return icon for the jminor logo in red
   */
  ImageIcon logoRed();

  /**
   * @return the active {@link Icons} instance.
   */
  static Icons icons() {
    return DefaultIcons.INSTANCE;
  }
}
