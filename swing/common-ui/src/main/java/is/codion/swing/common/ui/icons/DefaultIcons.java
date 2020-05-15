/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.icons;

import javax.swing.ImageIcon;
import java.awt.Toolkit;

public class DefaultIcons implements Icons {

  private static final String IMG_FILTER_16 = "Filter16.gif";
  private static final String IMG_PREFERENCES_16 = "Preferences16.gif";
  private static final String CODION_LOGO_BLACK_48 = "codion-logo-rounded-black-48x48.png";
  private static final String CODION_LOGO_TRANSPARENT_48 = "codion-logo-transparent-48x48.png";
  private static final String CODION_LOGO_RED_48 = "codion-logo-rounded-red-48x48.png";

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
    return imageIcon(CODION_LOGO_BLACK_48);
  }

  @Override
  public final ImageIcon logoTransparent() {
    return imageIcon(CODION_LOGO_TRANSPARENT_48);
  }

  @Override
  public final ImageIcon logoRed() {
    return imageIcon(CODION_LOGO_RED_48);
  }

  private static ImageIcon imageIcon(final String resourceName) {
    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(Icons.class.getResource(resourceName)));
  }
}
