/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icons;

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
   * @param ikon the ikon
   * @return a new FontImageIcon
   */
  static FontImageIcon of(Ikon ikon) {
    return new DefaultFontImageIcon(ikon);
  }

  /**
   * @param ikon the ikon
   * @param size the size
   * @param color the color
   * @return a new FontImageIcon
   */
  static FontImageIcon of(Ikon ikon, int size, Color color) {
    return new DefaultFontImageIcon(ikon, size, color);
  }
}
