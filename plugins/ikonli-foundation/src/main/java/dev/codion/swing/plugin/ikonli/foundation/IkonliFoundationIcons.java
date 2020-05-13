/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.plugin.ikonli.foundation;

import org.jminor.swing.common.ui.icons.DefaultIcons;
import org.jminor.swing.common.ui.icons.Icons;

import org.kordamp.ikonli.foundation.Foundation;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;

import static java.util.Objects.requireNonNull;

/**
 * {@link Icons} implementation based on ikonli-foundation.
 */
public class IkonliFoundationIcons implements Icons {

  public static final int ICON_SIZE = 16;

  private final Icons defaultIcons = new DefaultIcons();

  @Override
  public final ImageIcon filter() {
    return imageIcon(FontIcon.of(Foundation.FILTER, ICON_SIZE));
  }

  @Override
  public final ImageIcon configure() {
    return imageIcon(FontIcon.of(Foundation.WIDGET, ICON_SIZE));
  }

  @Override
  public final ImageIcon logo() {
    return defaultIcons.logo();
  }

  @Override
  public final ImageIcon logoRed() {
    return defaultIcons.logoRed();
  }

  /**
   * Creates a {@link ImageIcon} from the given icon.
   * @param icon the icon
   * @return a ImageIcon based on the given icon
   */
  public static ImageIcon imageIcon(final Icon icon) {
    requireNonNull(icon, "icon");
    final BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    icon.paintIcon(null, image.getGraphics(), 0, 0);

    return new ImageIcon(image);
  }
}
