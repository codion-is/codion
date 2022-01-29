/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icons;

import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import static java.util.Objects.requireNonNull;

public class DefaultIcons implements Icons {

  public static final int ICON_SIZE = 16;

  private static final String CODION_LOGO_BLACK_48 = "codion-logo-rounded-black-48x48.png";
  private static final String CODION_LOGO_TRANSPARENT_48 = "codion-logo-transparent-48x48.png";
  private static final String CODION_LOGO_RED_48 = "codion-logo-rounded-red-48x48.png";

  @Override
  public final ImageIcon filter() {
    return imageIcon(FontIcon.of(Ikons.FILTER, ICON_SIZE));
  }

  @Override
  public final ImageIcon configure() {
    return imageIcon(FontIcon.of(Ikons.CONFIGURE, ICON_SIZE));
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

  /**
   * Creates a {@link ImageIcon} from the given icon.
   * @param icon the icon
   * @return a ImageIcon based on the given icon
   */
  protected static ImageIcon imageIcon(final Icon icon) {
    requireNonNull(icon, "icon");
    final BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    icon.paintIcon(null, image.getGraphics(), 0, 0);

    return new ImageIcon(image);
  }

  private static ImageIcon imageIcon(final String resourceName) {
    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(Icons.class.getResource(resourceName)));
  }
}
