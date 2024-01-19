/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icon;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.image.BufferedImage;

import static java.util.Objects.requireNonNull;

/**
 * A FontImageIcon
 */
public interface FontImageIcon {

  /**
   * @return the Image Icon
   */
  ImageIcon imageIcon();

  /**
   * Sets the icon color
   * @param color the color
   */
  void color(Color color);

  /**
   * Paints a FontIcon onto an Image Icon
   */
  interface IconPainter {

    /**
     * Paints the given font icon onto the given image icon
     * @param fontIcon the font icon
     * @param imageIcon the image icon
     */
    default void paintIcon(FontIcon fontIcon, ImageIcon imageIcon) {
      requireNonNull(fontIcon).paintIcon(null, requireNonNull(imageIcon).getImage().getGraphics(), 0, 0);
    }
  }

  /**
   * Creates a ImageIcon on which to paint the FontIcon
   */
  interface ImageIconFactory {

    /**
     * Creates an ImageIcon for the given FontIcon
     * @param fontIcon the font icon
     * @return a new ImageIcon
     */
    default ImageIcon createImageIcon(FontIcon fontIcon) {
      return new ImageIcon(new BufferedImage(requireNonNull(fontIcon).getIconWidth(), fontIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB));
    }
  }

  /**
   * A builder for a FontImageIcon.
   */
  interface Builder {

    /**
     * @param size the font size
     * @return this builder
     */
    Builder size(int size);

    /**
     * @param color the font color
     * @return this builder
     */
    Builder color(Color color);

    /**
     * @param iconPainter the font painter
     * @return this builder
     */
    Builder iconPainter(IconPainter iconPainter);

    /**
     * @param imageIconFactory the icon factory
     * @return this builder
     */
    Builder imageIconFactory(ImageIconFactory imageIconFactory);

    /**
     * @return a new font imge icon
     */
    FontImageIcon build();
  }

  /**
   * @param ikon the ikon
   * @return a new {@link FontImageIcon.Builder}
   */
  static Builder builder(Ikon ikon) {
    return new DefaultFontImageIcon.DefaultBuilder(ikon);
  }
}
