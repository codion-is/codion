/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.icons;

import javax.swing.ImageIcon;
import java.awt.Toolkit;

public class DefaultIcons implements Icons {

  private static final String IMG_FILTER_16 = "Filter16.gif";
  private static final String IMG_PREFERENCES_16 = "Preferences16.gif";
  private static final String IMG_CODION_LOGO_BLACK_32 = "codion-logo-black-32x32.png";
  private static final String IMG_CODION_LOGO_BLACK_48 = "codion-logo-black-48x48.png";
  private static final String IMG_CODION_LOGO_BLACK_256 = "codion-logo-black-256x256.png";
  private static final String IMG_CODION_LOGO_RED_32 = "codion-logo-red-32x32.png";
  private static final String IMG_CODION_LOGO_RED_48 = "codion-logo-red-48x48.png";
  private static final String IMG_CODION_LOGO_RED_256 = "codion-logo-red-256x256.png";

  @Override
  public final ImageIcon filter() {
    return imageIcon(IMG_FILTER_16);
  }

  @Override
  public final ImageIcon configure() {
    return imageIcon(IMG_PREFERENCES_16);
  }

  @Override
  public final ImageIcon logoBlack() {
    return imageIcon(IMG_CODION_LOGO_BLACK_48);
  }

  @Override
  public final ImageIcon logoRed() {
    return imageIcon(IMG_CODION_LOGO_RED_48);
  }

  private static ImageIcon imageIcon(final String resourceName) {
    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(Icons.class.getResource(resourceName)));
  }
}
