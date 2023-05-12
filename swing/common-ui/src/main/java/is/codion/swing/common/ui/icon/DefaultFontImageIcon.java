/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icon;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * A default FontImageIcon implementation.
 */
public class DefaultFontImageIcon implements FontImageIcon {

  private final FontIcon fontIcon;
  private final ImageIcon imageIcon;

  protected DefaultFontImageIcon(Ikon ikon) {
    this(ikon, Icons.ICON_SIZE.get());
  }

  protected DefaultFontImageIcon(Ikon ikon, int size) {
    this(ikon, size, Icons.ICON_COLOR.get());
  }

  protected DefaultFontImageIcon(Ikon ikon, int size, Color color) {
    fontIcon = FontIcon.of(ikon, size, color);
    imageIcon = createImageIcon();
    paintIcon();
  }

  @Override
  public final ImageIcon imageIcon() {
    return imageIcon;
  }

  @Override
  public void setColor(Color color) {
    fontIcon.setIconColor(color);
    paintIcon();
  }

  protected final FontIcon fontIcon() {
    return fontIcon;
  }

  protected void paintIcon() {
    fontIcon.paintIcon(null, imageIcon.getImage().getGraphics(), 0, 0);
  }

  private ImageIcon createImageIcon() {
    return new ImageIcon(new BufferedImage(fontIcon.getIconWidth(), fontIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB));
  }
}
