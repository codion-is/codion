/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.icons;

import javax.swing.ImageIcon;
import java.awt.Toolkit;

final class DefaultIcons implements Icons {

  static final Icons INSTANCE = new DefaultIcons();

  private static final String IMG_FILTER_16 = "Filter16.gif";
  private static final String IMG_PREFERENCES_16 = "Preferences16.gif";
  private static final String IMG_JMINOR_LOGO_32 = "jminor_logo32.gif";
  private static final String IMG_JMINOR_LOGO_RED_24 = "jminor_logo_red24.png";

  @Override
  public ImageIcon filter() {
    return imageIcon(IMG_FILTER_16);
  }

  @Override
  public ImageIcon configure() {
    return imageIcon(IMG_PREFERENCES_16);
  }

  @Override
  public ImageIcon logo() {
    return imageIcon(IMG_JMINOR_LOGO_32);
  }

  @Override
  public ImageIcon logoRed() {
    return imageIcon(IMG_JMINOR_LOGO_RED_24);
  }

  private static ImageIcon imageIcon(final String resourceName) {
    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(Icons.class.getResource(resourceName)));
  }
}
