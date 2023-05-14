/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icon;

import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;
import java.awt.Color;

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
   * A builder for a {@link FontImageIcon}.
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
