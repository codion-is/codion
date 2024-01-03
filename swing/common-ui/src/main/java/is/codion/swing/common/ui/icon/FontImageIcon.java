/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
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
  void setColor(Color color);

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
